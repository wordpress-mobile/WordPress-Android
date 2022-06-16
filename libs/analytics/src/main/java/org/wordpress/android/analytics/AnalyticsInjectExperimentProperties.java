package org.wordpress.android.analytics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsInjectExperimentProperties {
   private Map<String, ?> mProperties;

   private AnalyticsInjectExperimentProperties(Map<String, ?> properties) {
      mProperties = (properties == null) ? Collections.emptyMap() : properties;
   }

   public Map<String, ?> getProperties() {
      return mProperties;
   }

   public void updateProperties(Map<String, ?> properties) {
      mProperties = properties;
   }

   public Map<String, ?> injectProperties(Map<String, ?> properties) {
      Map<String, Object> props = new HashMap<>();
      if (properties != null && properties.size() != 0) {
         props.putAll(properties);
      }
      getProperties().forEach(props::put);

      return props;
   }

   public static AnalyticsInjectExperimentProperties emptyInstance() {
      return new AnalyticsInjectExperimentProperties(Collections.emptyMap());
   }

   public static AnalyticsInjectExperimentProperties newInstance(Map<String, ?> properties) {
      return new AnalyticsInjectExperimentProperties(properties);
   }
}
