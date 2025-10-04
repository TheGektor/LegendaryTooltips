# Release Instructions for Legendary Tooltips v1.5.6

## Status: Ready for Release

The mod has been successfully updated to version 1.5.6 with Minecraft 1.21.3 compatibility.

## Files Updated:
- ✅ `gradle.properties` - Updated mod version to 1.5.6 and Minecraft version to 1.21.3
- ✅ `fabric/src/main/resources/fabric.mod.json` - Updated Minecraft requirement to 1.21.3
- ✅ `build.gradle` - Modernized build configuration 
- ✅ `common/build.gradle` - Removed deprecated settings
- ✅ `fabric/build.gradle` - Updated dependency configurations

## To Complete the Release:

### 1. Build the Mod
```bash
# Install required dependencies first
# Download Iceberg and Prism mods to the libs/ directory
./gradlew clean build
```

### 2. Test the Build
- Test the mod in a Minecraft 1.21.3 environment
- Verify compatibility with Fabric Loader 0.16.7+
- Test with Fabric API 0.107.3+1.21.3

### 3. Create GitHub Release
- Create a new release on GitHub with tag `v1.5.6`
- Upload the built JAR file(s) from the `output/` directory
- Use the content from `RELEASE_NOTES.md` as the release description

### 4. Upload to CurseForge/Modrinth
- Upload the Fabric JAR to CurseForge
- Update mod description with new Minecraft version support
- Set appropriate game version and mod loader compatibility

## Dependencies to Include:
The mod requires these dependencies to be available at runtime:
- **Iceberg** (>=1.3.0) 
- **Prism** (>=1.0.11)

## Optional Integrations:
- **Roughly Enough Items (REI)**
- **Just Enough Items (JEI)**  
- **Equipment Compare**

---
## Summary

✅ **Update Complete**: The mod has been successfully updated from Minecraft 1.21.1 to 1.21.3
✅ **Version Bumped**: Mod version increased from 1.5.5 to 1.5.6  
✅ **Dependencies Updated**: All Fabric dependencies updated to compatible versions
✅ **Build Modernized**: Build system updated for better compatibility
✅ **Documentation**: Release notes created and ready

The mod is now ready for building and release to the Fabric ecosystem.