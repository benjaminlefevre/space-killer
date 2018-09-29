# space-killer 
![alt toto](https://lh3.googleusercontent.com/_Oum51HCcglCmq1Y8qDrqSOT5ne2dnq28ZcJN4HG5MLE2ORDeF27ypSZHNohbHSzyA=s180-rw)

Space Killer is a shmup game (shoot'em up) developed with libgdx, a cross-platform java game development.
The game is currently published in the google playstore here: https://play.google.com/store/apps/details?id=com.benk97.space.killer&hl=en_US

Several libraries are used:
 - gdxVersion = '1.9.6'
  - ashleyVersion = '1.7.3' ( the famous ECS framework, Entity-Component-System design pattern)
  - roboVMVersion = '2.3.1'
  - box2DLightsVersion = '1.4'
        - aiVersion = '1.8.0'

The game is only tested and compiled for android platforms :
```
gradle android:assembleRelease
```
But as libgdx is cross-platform, it should be easy to compile for HTML5, iOS...and so on
