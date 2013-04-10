#!/bin/sh
rm -f ../GitGrab.jar
cp ../GitGrabCopy.jar . 
jar xvf GitGrabCopy.jar
rm GitGrabCopy.jar
cp classes/org/eclipse/jgit/internal/JGitText_en_US.properties org/eclipse/jgit/internal/JGitText_en_US.properties
jar cvfm GitGrabCopy.jar META-INF/MANIFEST.MF *
cp GitGrabCopy.jar ../GitGrab.jar
ls -d *|grep -v runall.sh | xargs rm -rf
