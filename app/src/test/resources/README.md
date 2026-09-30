# Detail number fixtures

Each `.argb.gz` file contains 171 × 47 pixels as gzip-compressed, big-endian
32-bit ARGB integers in row-major order. The fixture crops contain only the
number row, without account information.

- `number-reference-13392`: original saved detail number image.
- `number-live-13392`: a later live capture of the same number whose OCR was wrong.
- `number-reference-14072`: a different number used to reject a false match.

All three are crops of the development phone's 1220 × 2712 display, measured
on 2026-09-30. They test image equivalence, not recognition of arbitrary IDs
or other screen sizes. The original inventory remains unconfirmed.
