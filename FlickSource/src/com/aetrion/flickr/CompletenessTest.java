package com.aetrion.flickr;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Properties;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.AuthInterface;
import com.aetrion.flickr.reflection.ReflectionInterface;
import com.aetrion.flickr.util.IOUtilities;

/**
 * Tests the basic completeness of the api.
 * @author till (Till Krech) flickr:extranoise
 *
 */
public class CompletenessTest extends TestCase {

    Flickr flickr = null;
    Properties properties = null;
    Properties replacements;

    public CompletenessTest(String arg0) {
        super(arg0);
    }

	protected void setUp() throws Exception {
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("/setup.properties");
            properties = new Properties();
            properties.load(in);

            REST rest = new REST();

            flickr = new Flickr(
                properties.getProperty("apiKey"),
                properties.getProperty("secret"),
                rest
            );

            RequestContext requestContext = RequestContext.getRequestContext();

            AuthInterface authInterface = flickr.getAuthInterface();
            Auth auth = authInterface.checkToken(properties.getProperty("token"));
            requestContext.setAuth(auth);
        } finally {
            IOUtilities.close(in);
        }
        try {
            in = getClass().getResourceAsStream("/completenesstest.properties");
            replacements = new Properties();
            replacements.load(in);
        } finally {
            IOUtilities.close(in);
        }

	}
	
	public void testIfComplete() throws IOException, SAXException, FlickrException {
		ReflectionInterface ri = flickr.getReflectionInterface();
		Iterator mit = ri.getMethods().iterator();
		int notFound = 0;
		while (mit.hasNext()) {
			String method = (String)mit.next();
			if (!checkMethod(method)) {
				notFound++;
			}
		}
		assertEquals(0, notFound);
 	}
	
	private String getReplacement(String fullMethodName) {
		String repl = replacements.getProperty(fullMethodName);
		if (repl == null) {
			String meth = fullMethodName.substring(fullMethodName.lastIndexOf('.') + 1);
			Iterator keys = replacements.keySet().iterator();
			while (keys.hasNext()) {
				String key = (String)keys.next();
				if (key.endsWith(".*")) {
					String keyPack = key.substring(0, key.length()-2);
					String methPack = fullMethodName.substring(0, fullMethodName.lastIndexOf('.'));
					if (keyPack.equals(methPack)) {
						String cls = replacements.getProperty(key);
						if (cls.endsWith(".*")) {
							cls = cls.substring(0, cls.length() - 2);
						}
						repl = cls + "." + meth;
						break;
					}
				}
			}
		}
		return repl;
	}

	private boolean checkMethod(String fullMethodName) {
		String repl = getReplacement(fullMethodName);
		String methodName;
		String fqClassName;
		if (repl != null) {
			if ("skip".equals(repl)) {
				return true;
			}
			fqClassName = repl.substring(0, repl.lastIndexOf('.'));
			methodName = repl.substring(repl.lastIndexOf('.')+1);
		} else {
			int dotIdx = fullMethodName.lastIndexOf('.');
			String pack = fullMethodName.substring(0, dotIdx);
			methodName = fullMethodName.substring(dotIdx + 1);
			dotIdx = pack.lastIndexOf('.');
			String candidate = pack.substring(dotIdx+1);
			String javaPack = "com.aetrion." + pack;
			String className = Character.toUpperCase(candidate.charAt(0)) + candidate.substring(1) + "Interface";
			fqClassName = javaPack + "." + className;
		}
		boolean found = false;
		try {
			Class cl = Class.forName(fqClassName);
			Method[] javaMethods = cl.getMethods();
			for (int i = 0; i < javaMethods.length; i++) {
				if (javaMethods[i].getName().equals(methodName)) {
					found = true;
					break;
				}
			}
			if (!found) {
				System.out.println("ATTENTION: Method not implemented in flickrj: " + fqClassName + "." + methodName);
			}
		} catch (ClassNotFoundException e) {
			System.out.println("ATTENTION:  Class not implemented in flickrj: [" + fqClassName + "] (Method: " + methodName +")");
		}
		return found;
	}
}
