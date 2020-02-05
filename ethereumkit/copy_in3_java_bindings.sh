#!/u

# copy client to java path
cp -r in3-c/src/bindings/java/in3 src/main/java/
# but not the native libs
rm -rf src/main/java/in3/native
