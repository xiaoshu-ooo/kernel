#!/usr/bin/env python3
"""Extract the first APK v2 signer certificate and print size + SHA-256."""
import hashlib
import struct
import sys

APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"
V2_ID = 0x7109871A


def u32le(data, off):
    return struct.unpack_from("<I", data, off)[0]


def u64le(data, off):
    return struct.unpack_from("<Q", data, off)[0]


def find_eocd(data):
    # EOCD must be within the last 65557 bytes of a normal APK.
    start = max(0, len(data) - (65535 + 22))
    pos = data.rfind(b"PK\x05\x06", start)

    if pos < 0:
        raise RuntimeError("ZIP EOCD not found")

    return pos


def find_cert(apk):
    with open(apk, "rb") as f:
        data = f.read()

    if len(data) < 22:
        raise RuntimeError("APK is too small")

    eocd = find_eocd(data)

    # EOCD:
    # +16 = central directory offset
    cd_offset = u32le(data, eocd + 16)

    # APK Signing Block is immediately before the central directory.
    if cd_offset < 24:
        raise RuntimeError("Invalid central directory offset")

    magic_pos = cd_offset - 16

    if data[magic_pos:cd_offset] != APK_SIG_BLOCK_MAGIC:
        raise RuntimeError(
            "APK Signing Block not found "
            f"(central directory offset={cd_offset})"
        )

    # Footer:
    # uint64 size-including-size-field
    # 16-byte magic
    size = u64le(data, cd_offset - 24)

    block_start = cd_offset - (size + 8)

    if block_start < 0:
        raise RuntimeError("Invalid signing block size")

    if u64le(data, block_start) != size:
        raise RuntimeError("Signing block size mismatch")

    if data[block_start + size - 16:block_start + size] != APK_SIG_BLOCK_MAGIC:
        raise RuntimeError("Invalid signing block magic")

    # Pairs occupy:
    # [uint64 length][uint32 ID][value...]
    pos = block_start + 8
    end = cd_offset - 24

    while pos < end:
        if pos + 8 > end:
            raise RuntimeError("Truncated signing block pair")

        pair_len = u64le(data, pos)
        pos += 8

        if pair_len < 4:
            raise RuntimeError("Invalid signing block pair length")

        pair_end = pos + pair_len

        if pair_end > end:
            raise RuntimeError("Signing block pair out of range")

        ident = u32le(data, pos)
        value_start = pos + 4
        value_end = pair_end

        if ident == V2_ID:
            value = data[value_start:value_end]

            # APK v2 value:
            # signers sequence
            sa, sb = lp(value, 0, len(value))

            # First signer:
            signer = value[sa:sb]

            # signed data
            sda, sdb = lp(signer, 0, len(signer))
            signed_data = signer[sda:sdb]

            # signed data layout:
            # digests + certificates + additional attributes
            da, db = lp(signed_data, 0, len(signed_data))
            ca, cb = lp(signed_data, db, len(signed_data))

            certificates = signed_data[ca:cb]

            # First certificate
            cert_a, cert_b = lp(certificates, 0, len(certificates))
            cert = certificates[cert_a:cert_b]

            if not cert:
                raise RuntimeError("Empty APK v2 certificate")

            return cert

        pos = pair_end

    raise RuntimeError("APK v2 signer block not found")


def lp(blob, off, limit):
    if off + 4 > limit:
        raise RuntimeError("Truncated length-prefixed field")

    n = u32le(blob, off)
    a = off + 4
    b = a + n

    if b > limit:
        raise RuntimeError("Length-prefixed field out of range")

    return a, b


def main():
    if len(sys.argv) != 2:
        print(
            f"usage: {sys.argv[0]} APK",
            file=sys.stderr,
        )
        return 2

    apk = sys.argv[1]

    try:
        cert = find_cert(apk)
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    print(f"SIZE={len(cert)}")
    print(f"SHA256={hashlib.sha256(cert).hexdigest()}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
