package org.springframework.roo.addon.web.mvc.views;

import java.util.Collection;

import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;

/**
*
* This abstract class implements AbstractItdTypeDetailsProvidingMetadataItem class and
* provides a common point for all Metadatas for views
*
* @author Jose Manuel Vivó
* @since 2.0
*/
public abstract class AbstractViewMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected AbstractViewMetadata(String identifier, JavaType aspectName,
      PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
  }

  protected AbstractViewMetadata(String identifier, JavaType aspectName,
      PhysicalTypeMetadata governorPhysicalTypeMetadata, Collection<String> excludeMethods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata, excludeMethods);
  }


  public abstract JavaType getEntity();

  public abstract boolean isReadOnly();

  public abstract ControllerMetadata getControllerMetadata();

  /**
   * Informs if referenced view should be generated
   *
   * @param viewName
   * @return
   */
  public abstract boolean shouldGenerateView(String viewName);

}
