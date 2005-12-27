/**
 * $RCSfile$
 * $Revision: 1217 $
 * $Date: 2005-04-11 17:11:06 -0400 (Mon, 11 Apr 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger.net;

import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * A custom KeyManagerFactory that creates a key manager list using the
 * default key manager or a standard keystore as specified in jive_config.xml.
 * The default keystore provided with the Jive distribution uses the Sun Java
 * Keystore (JKS) and that takes a single password which must apply to both the
 * keystore and the key itself. Users may specify another keystore type and keystore
 * location. Alternatively, don't set a keystore type to use the JVM defaults and
 * configure your JVMs security files (see your JVM documentation) to plug in
 * any KeyManagerFactory provider.
 *
 * @author Iain Shigeoka
 */
public class SSLJiveKeyManagerFactory {

    /**
     * Creates a KeyManager list which is null if the storeType is null, or
     * is a standard KeyManager that uses a KeyStore of type storeType,
     * located at 'keystore' location under home, and uses 'keypass' as
     * the password for the keystore password and key password. The default
     * Jive keystore contains a self-signed X509 certificate pair under the
     * alias '127.0.0.1' in a Java KeyStore (JKS) with initial password 'changeit'.
     * This is sufficient for local host testing but should be using standard
     * key management tools for any significant testing or deployment. See
     * the Jive XMPP server security documentation for more information.
     *
     * @param storeType The type of keystore (e.g. "JKS") to use or null to indicate no keystore should be used
     * @param keystore  The relative location of the keystore under home
     * @param keypass   The password for the keystore and key
     * @return An array of relevant KeyManagers (may be null indicating a default KeyManager should be created)
     * @throws NoSuchAlgorithmException  If the keystore type doesn't exist (not provided or configured with your JVM)
     * @throws KeyStoreException         If the keystore is corrupt
     * @throws IOException               If the keystore could not be located or loaded
     * @throws CertificateException      If there were no certificates to be loaded or they are invalid
     * @throws UnrecoverableKeyException If they keystore coud not be opened (typically the password is bad)
     */
    public static KeyManager[] getKeyManagers(String storeType, String keystore, String keypass) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {
        KeyManager[] keyManagers;
        if (keystore == null) {
            keyManagers = null;
        }
        else {
            if (keypass == null) {
                keypass = "";
            }
            KeyStore keyStore = KeyStore.getInstance(storeType);
            keyStore.load(new FileInputStream(keystore), keypass.toCharArray());

            KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyFactory.init(keyStore, keypass.toCharArray());
            keyManagers = keyFactory.getKeyManagers();
        }
        return keyManagers;
    }
}
