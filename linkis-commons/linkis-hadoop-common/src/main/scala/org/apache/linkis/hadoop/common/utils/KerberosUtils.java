package org.apache.linkis.hadoop.common.utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION;
import static org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod.KERBEROS;

/**
 * @author leojie 2021/11/27 10:03 下午
 */
public class KerberosUtils {
    private static final Logger LOG = LoggerFactory.getLogger(KerberosUtils.class);

    private KerberosUtils() {
    }

    private static Configuration createKerberosSecurityConfiguration() {
        Configuration conf = new Configuration();
        conf.set(HADOOP_SECURITY_AUTHENTICATION, KERBEROS.toString());
        return conf;
    }

    public static void createKerberosSecureConfiguration(String keytab, String principal) {
        Configuration conf = createKerberosSecurityConfiguration();
        UserGroupInformation.setConfiguration(conf);
        try {
            if (!UserGroupInformation.isSecurityEnabled()
                    || UserGroupInformation.getCurrentUser().getAuthenticationMethod() != KERBEROS
                    || !UserGroupInformation.isLoginKeytabBased()) {
                UserGroupInformation.loginUserFromKeytab(principal, keytab);
                LOG.info("Login successfully with keytab: {} and principal: {}", keytab, principal);
            } else {
                LOG.info("The user has already logged in using keytab and principal, no action required");
            }
        } catch (IOException e) {
            LOG.error("Failed to get either keytab location or principal name in the jdbc executor", e);
        }
    }

    public static boolean runRefreshKerberosLogin() {
        Configuration conf = createKerberosSecurityConfiguration();
        UserGroupInformation.setConfiguration(conf);
        try {
            if (UserGroupInformation.isLoginKeytabBased()) {
                LOG.debug("Trying re-login from keytab");
                UserGroupInformation.getLoginUser().reloginFromKeytab();
                return true;
            } else if (UserGroupInformation.isLoginTicketBased()) {
                LOG.debug("Trying re-login from ticket cache");
                UserGroupInformation.getLoginUser().reloginFromTicketCache();
                return true;
            }
        } catch (Exception e) {
            LOG.error("Unable to run kinit for linkis jdbc executor", e);
        }
        LOG.debug("Neither Keytab nor ticket based login. runRefreshKerberosLoginWork() returning false");
        return false;
    }

    public static Long getKerberosRefreshInterval() {
        long refreshInterval;
        String refreshIntervalString = "86400000";
        // defined in linkis-env.sh, if not initialized then the default value is 86400000 ms (1d).
        if (System.getenv("LINKIS_JDBC_KERBEROS_REFRESH_INTERVAL") != null) {
            refreshIntervalString = System.getenv("LINKIS_JDBC_KERBEROS_REFRESH_INTERVAL");
        }
        try {
            refreshInterval = Long.parseLong(refreshIntervalString);
        } catch (NumberFormatException e) {
            LOG.error("Cannot get time in MS for the given string, " + refreshIntervalString
                    + " defaulting to 86400000 ", e);
            refreshInterval = 86400000L;

        }
        return refreshInterval;
    }

    public static Integer kinitFailTimesThreshold() {
        Integer kinitFailThreshold = 5;
        //defined in linkis-env.sh, if not initialized then the default value is 5.
        if (System.getenv("LINKIS_JDBC_KERBEROS_KINIT_FAIL_THRESHOLD") != null) {
            try {
                kinitFailThreshold = new Integer(System.getenv("LINKIS_JDBC_KERBEROS_KINIT_FAIL_THRESHOLD"));
            } catch (Exception e) {
                LOG.error("Cannot get integer value from the given string, " + System
                        .getenv("LINKIS_JDBC_KERBEROS_KINIT_FAIL_THRESHOLD") + " defaulting to " + kinitFailThreshold, e);
            }
        }
        return kinitFailThreshold;
    }
}
