How to create the self-signed keystore:

"%JAVA_HOME%\bin\keytool" -genkey -keyalg RSA -alias selfsigned -keystore test-https-keystore.jks -storepass password -validity 36000 -keysize 2048

Using the following password for the key:
password
