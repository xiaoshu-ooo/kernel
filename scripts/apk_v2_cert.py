#!/usr/bin/env python3

import hashlib
import struct
import sys

MAGIC = b"APK Sig Block 42"
V2_ID = 0x7109871A
V3_ID = 0xF05368C0


def u32(data, off):
    return struct.unpack_from("<I", data, off)[0]


def u64(data, off):
    return struct.unpack_from("<Q", data, off)[0]


def find_eocd(data):
    start = max(0, len(data) - (65535 + 22))
    pos = data.rfind(b"PK\x05\x06", start)

    if pos < 0:
        raise RuntimeError("ZIP EOCD not found")

    return pos


def find_signing_block(data):
    eocd = find_eocd(data)

    if eocd + 22 > len(data):
        raise RuntimeError("truncated ZIP EOCD")

    cd_offset = u32(data, eocd + 16)
    cd_size = u32(data, eocd + 12)

    # Normal APK: signing block is immediately before central directory.
    # The EOCD central-directory offset refers to the offset after the
    # signing block has been removed, so first try the standard location.
    candidates = []

    candidates.append(cd_offset)

    # Also account for ZIP64/offset adjustments and search backwards
    # around the central-directory signature.
    sig = b"PK\x01\x02"
    p = data.find(sig, max(0, len(data) - cd_size - 1024), len(data))

    if p >= 0:
        candidates.append(p)

    seen = set()

    for cd in candidates:
        if cd in seen:
            continue
        seen.add(cd)

        if cd < 24 or cd > len(data):
            continue

        footer = cd - 24

        if footer < 0:
            continue

        if data[footer + 8:footer + 24] != MAGIC:
            continue

        size = u64(data, footer)

        block_start = cd - size - 8

        if block_start < 0:
            continue

        if block_start + 8 > len(data):
            continue

        if u64(data, block_start) != size:
            continue

        if block_start + size + 8 != cd:
            continue

        if data[cd - 16:cd] != MAGIC:
            continue

        return block_start, size + 8, cd

    # Last resort: locate the APK Signing Block magic and validate it.
    # This handles APKs whose EOCD offset has been altered by ZIP tooling.
    magic_positions = []
    pos = 0

    while True:
        pos = data.find(MAGIC, pos)
        if pos < 0:
            break
        magic_positions.append(pos)
        pos += 1

    for magic_pos in reversed(magic_positions):
        footer = magic_pos - 8

        if footer < 0:
            continue

        size = u64(data, footer)

        block_start = magic_pos - size - 8

        if block_start < 0:
            continue

        if block_start + 8 > len(data):
            continue

        if u64(data, block_start) != size:
            continue

        if block_start + size + 8 != magic_pos + 16:
            continue

        return block_start, size + 8, magic_pos + 16

    raise RuntimeError("invalid APK Signing Block magic")


def get_signing_block(data):
    block_start, block_total, cd = find_signing_block(data)

    # Signing block:
    #
    # uint64 size
    # ID-value pairs
    # uint64 size
    # 16-byte magic
    #
    pair_start = block_start + 8
    pair_end = cd - 24

    if pair_end < pair_start:
        raise RuntimeError("invalid signing block range")

    pos = pair_start

    while pos < pair_end:
        if pos + 8 > pair_end:
            raise RuntimeError("truncated signing block pair")

        pair_size = u64(data, pos)
        pos += 8

        if pair_size < 4:
            raise RuntimeError("invalid signing block pair size")

        end = pos + pair_size

        if end > pair_end:
            raise RuntimeError("signing block pair exceeds block")

        pair_id = u32(data, pos)
        value_start = pos + 4

        yield pair_id, data[value_start:end]

        pos = end


def lp(data, off, limit):
    if off + 4 > limit:
        raise RuntimeError("truncated length-prefixed field")

    length = u32(data, off)
    start = off + 4
    end = start + length

    if end > limit:
        raise RuntimeError(
            f"length-prefixed field out of range: "
            f"start={start}, length={length}, limit={limit}"
        )

    return start, end


def extract_certificate_from_signer_block(value):
    # v2/v3 Signing Block value:
    #
    # signers ::= length-prefixed sequence
    # signer ::= length-prefixed signed-data
    #            length-prefixed signatures
    #            length-prefixed public-key
    #
    signers_start, signers_end = lp(value, 0, len(value))

    pos = signers_start

    # Actually the first 4 bytes contain the total signers sequence.
    # Parse the sequence itself.
    signers_end = signers_start + (signers_end - signers_start)

    if pos >= signers_end:
        raise RuntimeError("empty signers")

    signer_start, signer_end = lp(value, pos, signers_end)

    signed_data_start, signed_data_end = lp(
        value, signer_start, signer_end
    )

    signed_data = value[signed_data_start:signed_data_end]

    # signed-data:
    # digests
    # certificates
    # additional attributes
    #
    digests_start, digests_end = lp(
        signed_data, 0, len(signed_data)
    )

    certs_start, certs_end = lp(
        signed_data, digests_end, len(signed_data)
    )

    pos = certs_start

    cert_start, cert_end = lp(
        signed_data, pos, certs_end
    )

    cert = signed_data[cert_start:cert_end]

    if not cert:
        raise RuntimeError("empty signer certificate")

    return cert


def extract_certificate(apk):
    with open(apk, "rb") as f:
        data = f.read()

    for pair_id, value in get_signing_block(data):
        if pair_id not in (V2_ID, V3_ID):
            continue

        cert = extract_certificate_from_signer_block(value)

        size = len(cert)
        sha256 = hashlib.sha256(cert).hexdigest()

        return size, sha256

    raise RuntimeError("APK v2/v3 signer not found")


def main():
    if len(sys.argv) != 2:
        print(
            f"Usage: {sys.argv[0]} APK",
            file=sys.stderr,
        )
        return 2

    apk = sys.argv[1]

    try:
        size, sha256 = extract_certificate(apk)

        print(size)
        print(sha256)

    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
