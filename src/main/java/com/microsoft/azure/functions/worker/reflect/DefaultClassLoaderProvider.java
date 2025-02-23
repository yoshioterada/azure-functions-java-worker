package com.microsoft.azure.functions.worker.reflect;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.microsoft.azure.functions.worker.*;

/**
 * @author Kevin Hillinger Default implementation of the class loader provider
 */
public class DefaultClassLoaderProvider implements ClassLoaderProvider {
  public DefaultClassLoaderProvider() {
    customerUrls = Collections.newSetFromMap(new ConcurrentHashMap<URL, Boolean>());
    workerUrls = Collections.newSetFromMap(new ConcurrentHashMap<URL, Boolean>());
  }

  /*
   * @see com.microsoft.azure.functions.reflect.ClassLoaderProvider#createClassLoader()
   */
  @Override
  public ClassLoader createClassLoader() {
    List<URL> urlsList = new ArrayList<>();
    urlsList.addAll(customerUrls);
    urlsList.addAll(workerUrls);
    URL[] urlsForClassLoader = urlsList.toArray(new URL[0]);
    URLClassLoader classLoader = new URLClassLoader(urlsForClassLoader);
    Thread.currentThread().setContextClassLoader(classLoader);
    return classLoader;
  }

  @Override
  public void addCustomerUrl(URL url) throws IOException {
    if (customerUrls.contains(url)) {
      return;
    }
    WorkerLogManager.getSystemLogger().info("Loading customer file URL: " + url);
    customerUrls.add(url);
    addUrlToSystemClassLoader(url);
  }

  @Override
  public void addWorkerUrl(URL url) throws IOException {
    if (workerUrls.contains(url)) {
      return;
    }
    WorkerLogManager.getSystemLogger().info("Loading worker file URL: " + url);
    workerUrls.add(url);
    addUrlToSystemClassLoader(url);
  }

  public static boolean isUrlPointingToAFile(URL url) throws UnsupportedEncodingException {
    String decodedPath = URLDecoder.decode(url.getPath(), "UTF-8");
    File file = new File(decodedPath);
    return file.exists();
  }

  private void addUrlToSystemClassLoader(URL url) throws IOException {
    URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class<?> sysclass = URLClassLoader.class;

    try {
      Method method = sysclass.getDeclaredMethod(SYS_LOADER_ADDURL_METHOD_NAME, parameters);
      method.setAccessible(true);
      method.invoke(sysloader, new Object[] { url });
    } catch (Throwable t) {
      throw new IOException("Error adding " + url + " to system classloader");
    }

  }

  private static final String SYS_LOADER_ADDURL_METHOD_NAME = "addURL";
  private static final Class<?>[] parameters = new Class[] { URL.class };
  private final Set<URL> customerUrls;
  private final Set<URL> workerUrls;
}
