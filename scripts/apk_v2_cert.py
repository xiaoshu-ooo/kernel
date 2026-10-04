#!/usr/bin/env python3
"""Extract the first APK v2 signer certificate and print size + SHA-256."""
import struct, sys, hashlib, zipfile

APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"
V2_ID = 0x7109871a


def u32le(b): return struct.unpack_from('<I', b)[0]
def u64le(b): return struct.unpack_from('<Q', b)[0]


def find_cert(apk):
    with open(apk, 'rb') as f:
        data = f.read()
    eocd = data.rfind(b'PK\x05\x06')
    if eocd < 0:
        raise RuntimeError('ZIP EOCD not found')
    cd_offset = u32le(data[eocd+16:eocd+20])
    # APK Signing Block sits immediately before central directory. Its footer
    # is: uint64 size_including_size_field, 16-byte magic.
    if cd_offset < 24 or data[cd_offset-16:cd_offset] != APK_SIG_BLOCK_MAGIC:
        raise RuntimeError('APK Signing Block not found')
    size = u64le(data[cd_offset-24:cd_offset-16])
    block_start = cd_offset - (size + 8)
    if block_start < 0:
        raise RuntimeError('Invalid signing block size')
    if u64le(data[block_start:block_start+8]) != size:
        raise RuntimeError('Signing block size mismatch')

    pos = block_start + 8
    end = cd_offset - 24
    while pos < end:
        pair_len = u64le(data[pos:pos+8]); pos += 8
        pair_end = pos + pair_len
        if pair_end > end or pair_len < 4:
            raise RuntimeError('Invalid signing block pair')
        ident = u32le(data[pos:pos+4]); value = data[pos+4:pair_end]; pos = pair_end
        if ident != V2_ID:
            continue
        # signer sequence: len-prefixed signer -> signed data -> digests,
        # certificates sequence -> first certificate.
        def lp(blob, off, limit):
            if off + 4 > limit: raise RuntimeError('truncated length prefix')
            n = u32le(blob[off:off+4]); a = off + 4; b = a + n
            if b > limit: raise RuntimeError('length-prefixed field out of range')
            return a, b
        a, b = lp(value, 0, len(value))
        sa, sb = lp(value, a, b)
        da, db = lp(value, sa, sb)
        # signed data = digests + certificates + attrs
        xa, xb = lp(value, da, db)
        ca, cb = lp(value, xb, db)
        cert_a, cert_b = lp(value, ca, cb)
        cert = value[cert_a:cert_b]
        return cert
    raise RuntimeError('APK v2 signer block not found')


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(f'usage: {sys.argv[0]} APK', file=sys.stderr); sys.exit(2)
    cert = find_cert(sys.argv[1])
    print(f'SIZE={len(cert)}')
    print(f'SHA256={hashlib.sha256(cert).hexdigest()}')
