   # An Android FPS Detect Tool
   
   ## Get it
   Step 1. Add the JitPack repository to your build file
   Add it in your root build.gradle at the end of repositories:
   ```
   dependencies {
   	        implementation 'com.github.AaronFang123:FpsDetector:0.0.1'
   	}
   ```
   Step 2. Add the dependency
   ```
   dependencies {
   	        implementation 'com.github.AaronFang123:FpsDetector:Tag'
   	}
   ```
   [![](https://jitpack.io/v/AaronFang123/FpsDetector.svg)](https://jitpack.io/#AaronFang123/FpsDetector)
   
   ## Usage
   ### FPS measurement for specific scenes
   Where to start:`FpsDetector.start()`, Where to end:`FpsDetector.stop()`
   Log TAG： FPS-DETECTOR
   
   ### Full FPS measurement
   Application:
   ```
   1. MainThreadMonitor.getMonitor().init();
   2. FullFpsTracker.init();
   
   ```
