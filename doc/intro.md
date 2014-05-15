# Introduction to deploy

## Overview

## Build

    $ lein deps
    $ lein uberjar

## Running


    $ java

## Setting up SSL Key Store on the Client

    # Create the key store
    #
    $ keytool -genkey -keyalg RSA -validity 5000 -alias <Client private key alias> -keystore client-keystore.jks \
      -dname "cn=client" -keypass <Private key password> -storepass <KeyStore password>
    #
    # Verify the key store creation
    #
    $ keytool -list -keystore client-keystore.jks -storepass <KeyStore password>
    #
    # Sign the key to make it usable
    #
    $ keytool -selfcert -validity 5000 -alias <Client private key alias> -keystore client-keystore.jks \
      -keypass <Private key password> -storepass <KeyStore password>
    #
    # Export the client public key
    #
    $ keytool -export -alias <Client private key alias> -keystore client-keystore.jks \
      -file client.pubkey -keypass <Private key password> -storepass <KeyStore password>
