# Visual acceptance contract

The user's explicit execution feedback requires staying very close to `design/previews/`. The previews are the acceptance baseline, not loose inspiration. Both implementor (GPT-6 Luna high) and reviewer (GPT-6.1 high) were instructed to open the actual images and compare native captures directly.

## References

- `design/previews/app-screens-dark.png`
- `design/previews/app-screens-light.png`
- `design/previews/live-above-limit.png`
- `design/previews/live-signal-lost.png`
- `design/previews/components-dark.png`
- `design/previews/components-light.png`

## Required fidelity

- Header: mark and PACEMAKER only, plus the reference's settings affordance. The invented SHARED RHYTHM subtitle must be removed from the app.
- Navigation: three direct destinations Live, Devices, Profile as requested during execution. Follow the updated guide's full item selection treatment and icon/label hierarchy.
- Live: small Your heart rate eyebrow, left-aligned large reading with bpm alongside, concise status, thin flat meter and limit marker, group rows, compact full-width speech selector and source footer. Avoid a giant centered metric card, extra title, repeated status messages or an always-visible edit slider.
- Member rows: square neutral initials tile with identity stripe, name/limit details, aligned reading with bpm below and row separators.
- Devices: Your signal title, the reference's source controls/device cards and scanning notice. Derive connection/selection/availability truthfully from real state.
- Profile: Make it yours title, name, centered limit with bordered decrement/increment controls, and the reference's spacing and control treatment.
- Status notices: flat semantic surfaces with a vertical colored side rule; avoid extra floating/rounded cards.
- Preserve the reference colors, fonts, spacing, alignment, content hierarchy and modest shape radii. Material defaults must not introduce an unrelated visual style.

## Functional adaptations

Three speech modes remain available through a compact selector. The existing editable limit range remains 40–200. Missing, expired, unavailable and equality states must remain truthful. Identity-color selection and existing immediate persistence remain accessible. Android system/IME/cutout insets, font scaling and accessibility can require adjustments; record the concrete reason for any material visual deviation.

Behavioral code approval does not imply visual approval. Final acceptance requires actual native captures compared with the references, including both themes and content below the fold. Controlled fixtures must provide the same themed shell/background as production so screenshots do not create false contrast defects.
