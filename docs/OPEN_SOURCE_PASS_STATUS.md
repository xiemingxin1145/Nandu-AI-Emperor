# Open-source audio/framework pass status

- Vetted CC0 audio has been materialized into this branch.
- Legacy BGM and high-frequency UI prompt clips are replaced.
- Packaged `assets/audio/voice/` is intentionally empty until explicit NPC voice routing lands.
- Utility AI primitive and architecture references are included.
- This marker commit intentionally does not touch the audio-sync workflow trigger paths, so the PR validation run can test the already-materialized assets without a competing push-side sync.
