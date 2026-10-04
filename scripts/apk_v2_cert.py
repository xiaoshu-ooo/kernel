#!/usr/bin/env python3
"""Extract the first APK v2/v3 signer certificate and print size + SHA-256."""

import hashlib
import struct
import sys


APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"
V2_ID = 0x7109871A
V3_ID = 0xF05368C0


def u32(data, offset):
    return struct.unpack_from("<I", data, offset)[0]


def u64(data, offset):
    return struct.unpack_from("<Q", data, offset)[0]


def read_lp(data, offset, limit):
    """Read a uint32-length-prefixed blob."""
    if offset + 4 > limit:
        raise RuntimeError("truncated length-prefixed field")

    length = u32(data, offset)
    start = offset + 4
    end = start + length

    if end > limit:
        raise RuntimeError(
            f"length-prefixed field out of range: "
            f"start={start}, end={end}, limit={limit}"
        )

    return data[start:end], end


def find_eocd(data):
    # EOCD must be in the last 65557 bytes for a normal ZIP/APK.
    start = max(0, len(data) - 65557)
    pos = data.rfind(b"PK\x05\x06", start)

    if pos < 0:
        raise RuntimeError("ZIP EOCD not found")

    return pos


def find_signing_block(data, central_dir_offset):
    """
    APK Signing Block:

        uint64 size
        ID-value pairs
        uint64 size
        16-byte magic

    The block is immediately before the ZIP Central Directory.
    """

    if central_dir_offset < 24:
        raise RuntimeError("invalid central directory offset")

    footer_offset = central_dir_offset - 24

    # Footer:
    #   uint64 size
    #   16-byte magic
    magic = data[footer_offset + 8:central_dir_offset]

    if magic != APK_SIG_BLOCK_MAGIC:
        raise RuntimeError(
            "APK Signing Block magic not found "
            f"(central directory offset={central_dir_offset})"
        )

    size = u64(data, footer_offset)

    # Size excludes the first uint64 size field.
    block_start = central_dir_offset - size - 8

    if block_start < 0:
        raise RuntimeError("invalid APK Signing Block size")

    if block_start + 8 > len(data):
        raise RuntimeError("APK Signing Block header out of range")

    header_size = u64(data, block_start)

    if header_size != size:
        raise RuntimeError(
            f"APK Signing Block size mismatch: "
            f"header={header_size}, footer={size}"
        )

    # Confirm the footer belongs to this block.
    if data[block_start + size - 16:block_start + size] != APK_SIG_BLOCK_MAGIC:
        raise RuntimeError("invalid APK Signing Block magic")

    # Pairs start immediately after the first uint64 size.
    pairs_start = block_start + 8

    # The footer is 24 bytes.
    pairs_end = central_dir_offset - 24

    if pairs_start > pairs_end:
        raise RuntimeError("empty APK Signing Block")

    return pairs_start, pairs_end


def extract_certificate(data, pairs_start, pairs_end):
    pos = pairs_start

    while pos < pairs_end:
        if pos + 8 > pairs_end:
            raise RuntimeError("truncated APK Signing Block pair")

        pair_length = u64(data, pos)
        pos += 8

        if pair_length < 4:
            raise RuntimeError("invalid APK Signing Block pair length")

        pair_end = pos + pair_length

        if pair_end > pairs_end:
            raise RuntimeError("APK Signing Block pair out of range")

        pair_id = u32(data, pos)
        value_start = pos + 4
        value = data[value_start:pair_end]

        if pair_id not in (V2_ID, V3_ID):
            pos = pair_end
            continue

        # v2/v3 value:
        #
        # signers = length-prefixed sequence
        signers, _ = read_lp(value, 0, len(value))

        # First signer.
        signer, _ = read_lp(signers, 0, len(signers))

        # signer:
        #   signedData
        #   signatures
        #   publicKey
        signed_data, signer_pos = read_lp(
            signer,
            0,
            len(signer),
        )

        # signedData:
        #   digests
        #   certificates
        #   additionalAttributes
        digests, pos2 = read_lp(
            signed_data,
            0,
            len(signed_data),
        )

        certificates, pos3 = read_lp(
            signed_data,
            pos2,
            len(signed_data),
        )

        # First certificate.
        certificate, _ = read_lp(
            certificates,
            0,
            len(certificates),
        )

        if not certificate:
            raise RuntimeError("empty APK signer certificate")

        return certificate

    raise RuntimeError("APK v2/v3 signer block not found")


def find_certificate(apk):
    with open(apk, "rb") as f:
        data = f.read()

    if len(data) < 22:
        raise RuntimeError("APK is too small")

    eocd = find_eocd(data)

    # ZIP EOCD +16 = central directory offset.
    central_dir_offset = u32(data, eocd + 16)

    # This script handles normal APK ZIP layout.
    if central_dir_offset == 0xFFFFFFFF:
        raise RuntimeError("ZIP64 APK is not supported")

    pairs_start, pairs_end = find_signing_block(
        data,
        central_dir_offset,
    )

    return extract_certificate(
        data,
        pairs_start,
        pairs_end,
    )


def main():
    if len(sys.argv) != 2:
        print(
            f"usage: {sys.argv[0]} APK",
            file=sys.stderr,
        )
        return 2

    try:
        certificate = find_certificate(sys.argv[1])
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    print(f"SIZE={len(certificate)}")
    print(
        f"SHA256={hashlib.sha256(certificate).hexdigest()}"
    )

    return 0


if __name__ == "__main__":
    sys.exit(main())
