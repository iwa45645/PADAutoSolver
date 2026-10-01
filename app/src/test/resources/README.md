# Detail number fixtures

Each `number-*.argb.gz` file contains 171 × 47 pixels as gzip-compressed, big-endian
32-bit ARGB integers in row-major order. The fixture crops contain only the
number row, without account information.

- `number-reference-13392`: original saved detail number image.
- `number-live-13392`: a later live capture of the same number whose OCR was wrong.
- `number-reference-14072`: a different number used to reject a false match.

All three are crops of the development phone's 1220 × 2712 display, measured
on 2026-09-30. They test image equivalence, not recognition of arbitrary IDs
or other screen sizes. The original inventory remains unconfirmed.

## Fixed team fixtures (2026-10-02)

`preentry-slot-*.argb.gz` contains 48 × 32 ARGB pixels in the same format,
cropped from the six portraits on the real pre-entry screen. These reject
swapped slots; they do not verify assists, latents or skill readiness.
The app's PNG references retain their original crop size so both reference
and live crops use Android's identical resizing operation.

`preentry-ocr-20261002.json` is real ML Kit output limited to the dungeon
header, target title and entry control. It reproduces the missing Japanese
particle without including account information.

`team-info-17.png` and `.json` preserve the game's displayed 3 + 7×2 = 17,
seal resistance 6 and the checked helper/super-awakening options. The helper
was not selected on that own-team screen. This is historical display evidence,
not proof of today's full-team aggregate or permission to enter a dungeon.

`fixed-team-profile.json` is the user's locked composition. The helper's
assist/latents stay null there. `helper-observed-20261002.json` is a single
observed helper sample for audit only and must never replace runtime checks.
