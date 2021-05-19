## Internal build

# Inside StarOS project root
You can build telegram_calls project from StarOS project root the same way as any other app, no extra actions needed:
```
./gradlew :telegram_calls:assembleDebug
```

# Outside of StarOS project root
To build outside of StarOS project root, you need to copy prebuilt versions of internal libraries into `TMessagesProj/libs` folder (for internal builds, they are generated into `TMessagesProj/build/input_aar_libs`). Also it is important to mention - you need to use system-wide `gradle` command for external build, as due to internal reasons (we can't launch external build on internal CI, due to restricted internet access on CI workers), `./gradlew` is not shipped with the project:
```
gradle assembleDebug
```
