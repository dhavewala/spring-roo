package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.RooJavaType.ROO_THYMELEAF;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMVCService;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.views.AbstractViewGeneratorMetadataProvider;
import org.springframework.roo.addon.web.mvc.views.MVCViewGenerationService;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringEnumDetails;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ThymeleafMetadataProvider}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ThymeleafMetadataProviderImpl extends AbstractViewGeneratorMetadataProvider implements
    ThymeleafMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ThymeleafMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  private JavaType globalSearchType;
  private JavaType datatablesDataType;

  private ControllerMVCService controllerMVCService;
  private MVCViewGenerationService viewGenerationService;

  private List<JavaType> typesToImport;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_THYMELEAF} as additional 
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_THYMELEAF);
  }

  /**
   * This service is being deactivated so unregister upstream-downstream 
   * dependencies, triggers, matchers and listeners.
   * 
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.removeNotificationListener(this);
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
        getProvidesType());
    this.registryTracker.close();

    removeMetadataTrigger(ROO_THYMELEAF);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ThymeleafMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = ThymeleafMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ThymeleafMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Thymeleaf";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any metadata is even
    // hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();
    final String localMid = domainTypeToServiceMidMap.get(governor);
    if (localMid != null) {
      return localMid;
    }

    final MemberHoldingTypeDetails memberHoldingTypeDetails =
        getTypeLocationService().getTypeDetails(governor);
    if (memberHoldingTypeDetails != null) {
      for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
        final String localMidType = domainTypeToServiceMidMap.get(type);
        if (localMidType != null) {
          return localMidType;
        }
      }
    }
    return null;
  }

  @Override
  protected MVCViewGenerationService getViewGenerationService() {
    if (viewGenerationService == null) {
      // Get all Services implement MVCViewGenerationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MVCViewGenerationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          MVCViewGenerationService viewService =
              (MVCViewGenerationService) this.context.getService(ref);
          if (viewService.getName().equals("THYMELEAF")) {
            viewGenerationService = viewService;
            return viewGenerationService;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MVCViewGenerationService on ThymeleafMetadataProviderImpl.");
        return null;
      }
    } else {
      return viewGenerationService;
    }
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem createMetadataInstance() {

    this.typesToImport = new ArrayList<JavaType>();

    // Getting service details
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(getService());

    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), logicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

    // Getting Global search class
    Set<ClassOrInterfaceTypeDetails> globalSearchClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_GLOBAL_SEARCH);
    if (globalSearchClasses.isEmpty()) {
      throw new RuntimeException("ERROR: GlobalSearch.java file doesn't exist or has been deleted.");
    }
    Iterator<ClassOrInterfaceTypeDetails> gobalSearchClassIterator = globalSearchClasses.iterator();
    while (gobalSearchClassIterator.hasNext()) {
      this.globalSearchType = gobalSearchClassIterator.next().getType();
      break;
    }

    // Getting DatatablesDataType
    Set<ClassOrInterfaceTypeDetails> datatablesDataClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_THYMELEAF_DATATABLES_DATA);
    if (datatablesDataClasses.isEmpty()) {
      throw new RuntimeException(
          "ERROR: DatatablesData.java file doesn't exist or has been deleted.");
    }
    Iterator<ClassOrInterfaceTypeDetails> datatablesDataClassIterator =
        datatablesDataClasses.iterator();
    while (datatablesDataClassIterator.hasNext()) {
      this.datatablesDataType = datatablesDataClassIterator.next().getType();
      break;
    }

    // Getting methods from related service
    MethodMetadata serviceSaveMethod = serviceMetadata.getSaveMethod();
    MethodMetadata serviceDeleteMethod = serviceMetadata.getDeleteMethod();
    MethodMetadata serviceFindAllGlobalSearchMethod =
        serviceMetadata.getFindAllGlobalSearchMethod();
    MethodMetadata serviceCountMethod = serviceMetadata.getCountMethod();

    return new ThymeleafMetadata(metadataIdentificationString, this.aspectName,
        this.governorPhysicalTypeMetadata, getListFormMethod(),
        getListJSONMethod(serviceFindAllGlobalSearchMethod),
        getListDatatablesJSONMethod(serviceCountMethod), getCreateFormMethod(),
        getCreateMethod(serviceSaveMethod), getEditFormMethod(),
        getUpdateMethod(serviceSaveMethod), getDeleteMethod(serviceDeleteMethod), getShowMethod(),
        getPopulateFormMethod(), isReadOnly(), typesToImport);
  }

  /**
   * This method provides the "list" JSON method using JSON 
   * response type and returns Page element
   * 
   * @param serviceFindAllGlobalSearchMethod
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getListJSONMethod(MethodMetadata serviceFindAllGlobalSearchMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(this.controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("list");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.globalSearchType));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.PAGEABLE));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Page<Entity> entityField = serviceField.findAll(search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), addTypeToImport(this.entity)
            .getSimpleTypeName(), getEntityField().getFieldName(),
        getServiceField().getFieldName(), serviceFindAllGlobalSearchMethod.getMethodName()));

    // return entityField;
    bodyBuilder.appendFormalLine(String.format("return %s;", getEntityField().getFieldName()));


    // Generating returnType
    JavaType returnType =
        new JavaType(SpringJavaType.PAGE.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(this.entity));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" Datatables JSON method  using JSON 
   * response type and returns Datatables element
   * 
   * @param serviceCountMethod
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getListDatatablesJSONMethod(MethodMetadata serviceCountMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, "", "application/vnd.datatables+json",
            "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("list");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.globalSearchType));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.PAGEABLE));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType.INT_OBJECT));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));
    parameterNames.add(new JavaSymbolName("draw"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, "", "application/vnd.datatables+json", ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Page<Entity> entityField = list(search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = list(search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), addTypeToImport(this.entity)
            .getSimpleTypeName(), getEntityField().getFieldName()));

    // long allAvailableEntity = serviceField.count();
    bodyBuilder.appendFormalLine(String.format("long allAvailable%s = %s.%s();",
        this.entity.getSimpleTypeName(), getServiceField().getFieldName(),
        serviceCountMethod.getMethodName()));

    // return new DatatablesData<Entity>(entityField, allAvailableEntity, draw);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(%s, allAvailable%s, draw);",
        addTypeToImport(this.datatablesDataType).getSimpleTypeName(),
        this.entity.getSimpleTypeName(), getEntityField().getFieldName(),
        this.entity.getSimpleTypeName()));

    // Generating returnType
    JavaType returnType =
        new JavaType(this.datatablesDataType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(this.entity));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" form method  using Thymeleaf view 
   * response type
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getListFormMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("list");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return "path/list";
    bodyBuilder.appendFormalLine(String.format("return \"%s/list\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" form method  using Thymeleaf view 
   * response type
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getCreateFormMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/create-form", null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("createForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/create-form", null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // model.addAttribute(new Entity());
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(new %s());",
        this.entity.getSimpleTypeName()));

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/create\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" method  using Thymeleaf view 
   * response type
   * 
   * @param serviceSaveMethod MethodMetadata
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getCreateMethod(MethodMetadata serviceSaveMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_POST, "", null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("create");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        new JavaType("javax.validation.Valid")).build(), new AnnotationMetadataBuilder(
        SpringJavaType.MODEL_ATTRIBUTE).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.REDIRECT_ATTRIBUTES));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("result"));
    parameterNames.add(new JavaSymbolName("redirectAttrs"));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_POST, "", null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/create\";", getViewsPath()));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // Entity newEntity = entityService.SAVE_METHOD(entityField);
    bodyBuilder.appendFormalLine(String.format("%s new%s = %s.%s(%s);", this.entity
        .getSimpleTypeName(), this.entity.getSimpleTypeName(), getServiceField().getFieldName(),
        serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));


    // redirectAttrs.addAttribute("id", newEntity.ACCESSOR_METHOD());
    bodyBuilder.appendFormalLine(String.format("redirectAttrs.addAttribute(\"id\", new%s.%s());",
        this.entity.getSimpleTypeName(), this.identifierAccessor.getMethodName()));

    // return "redirect:/path/{id}";
    bodyBuilder.appendFormalLine(String.format("return \"redirect:/%s/{id}\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "edit" form method  using Thymeleaf view 
   * response type
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getEditFormMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET,
            String.format("/{%s}/edit-form", getEntityField().getFieldName()), null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("editForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.PATH_VARIABLE).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET,
        String.format("/{%s}/edit-form", getEntityField().getFieldName()), null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/edit\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "update" method  using Thymeleaf view 
   * response type
   * 
   * @param serviceSaveMethod MethodMetadata
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getUpdateMethod(MethodMetadata serviceSaveMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_PUT,
            String.format("/{%s}", getEntityField().getFieldName()), null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("update");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        new JavaType("javax.validation.Valid")).build(), new AnnotationMetadataBuilder(
        SpringJavaType.MODEL_ATTRIBUTE).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.REDIRECT_ATTRIBUTES));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("result"));
    parameterNames.add(new JavaSymbolName("redirectAttrs"));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_PUT,
        String.format("/{%s}", getEntityField().getFieldName()), null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/edit\";", getViewsPath()));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // Entity savedEntity = entityService.SAVE_METHOD(entityField);
    bodyBuilder.appendFormalLine(String.format("%s saved%s = %s.%s(%s);", this.entity
        .getSimpleTypeName(), this.entity.getSimpleTypeName(), getServiceField().getFieldName(),
        serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));

    // redirectAttrs.addAttribute("id", savedEntity.ACCESSOR_METHOD());
    bodyBuilder.appendFormalLine(String.format("redirectAttrs.addAttribute(\"id\", saved%s.%s());",
        this.entity.getSimpleTypeName(), this.identifierAccessor.getMethodName()));

    // return "redirect:/path/{id}";
    bodyBuilder.appendFormalLine(String.format("return \"redirect:/%s/{id}\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "delete" method using Thymeleaf view 
   * response type
   * 
   * @param serviceDeleteMethod
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getDeleteMethod(MethodMetadata serviceDeleteMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("delete");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", "id");

    parameterTypes.add(new AnnotatedJavaType(this.identifierType, pathVariableAnnotation.build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // entityService.DELETE_METHOD(id);
    bodyBuilder.appendFormalLine(String.format("%s.%s(id);", getServiceField().getFieldName(),
        serviceDeleteMethod.getMethodName()));

    // return "redirect:/path";
    bodyBuilder.appendFormalLine(String.format("return \"redirect:/%s\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "show" method  using Thymeleaf view 
   * response type
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getShowMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET,
            String.format("/{%s}", getEntityField().getFieldName()), null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("show");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.PATH_VARIABLE).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET,
        String.format("/{%s}", getEntityField().getFieldName()), null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return "path/show";
    bodyBuilder.appendFormalLine(String.format("return \"%s/show\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "populateForm" method 
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getPopulateFormMethod() {

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("populateForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    // Check if exists other populateForm method in this controller
    MemberDetails controllerMemberDetails = getMemberDetails(this.controller);
    MethodMetadata existingMethod =
        controllerMemberDetails.getMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting all enum types from provided entity
    MemberDetails entityDetails =
        getMemberDetails(getTypeLocationService().getTypeDetails(this.entity));
    List<FieldMetadata> fields = entityDetails.getFields();
    for (FieldMetadata field : fields) {
      if (isEnumType(field.getFieldType())) {
        // model.addAttribute("enumField", Arrays.asList(Enum.values()));
        bodyBuilder.appendFormalLine(String.format(
            "model.addAttribute(\"%s\", %s.asList(%s.values()));",
            Noun.pluralOf(field.getFieldName().getSymbolName(), Locale.ENGLISH),
            addTypeToImport(new JavaType("java.util.Arrays")).getSimpleTypeName(),
            addTypeToImport(field.getFieldType()).getSimpleTypeName()));
      }
    }

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method checks if the provided type is enum or not
   * 
   * @param fieldType
   * @return
   */
  private boolean isEnumType(JavaType fieldType) {
    Validate.notNull(fieldType, "Java type required");
    final ClassOrInterfaceTypeDetails javaTypeDetails =
        getTypeLocationService().getTypeDetails(fieldType);
    if (javaTypeDetails != null) {
      if (javaTypeDetails.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This method returns entity field included on controller
   * 
   * @return
   */
  private FieldMetadata getEntityField() {

    // Generating entity field name
    String fieldName =
        new JavaSymbolName(this.entity.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.entity)
        .build();
  }

  /**
   * This method returns service field included on controller
   * 
   * @return
   */
  private FieldMetadata getServiceField() {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(this.controller.getDeclaredByMetadataId());
    final String controllerMetadataKey =
        ControllerMetadata.createIdentifier(this.controller.getType(), logicalPath);
    registerDependency(controllerMetadataKey, metadataIdentificationString);
    final ControllerMetadata controllerMetadata =
        (ControllerMetadata) getMetadataService().get(controllerMetadataKey);

    return controllerMetadata.getServiceField();
  }


  /**
   * This method returns the final views path to be used
   * 
   * @return
   */
  private String getViewsPath() {
    return this.controllerPath.startsWith("/") ? this.controllerPath.substring(1)
        : this.controllerPath;
  }

  /**
   * This method registers a new type on types to import list
   * and then returns it.
   * 
   * @param type
   * @return
   */
  private JavaType addTypeToImport(JavaType type) {
    typesToImport.add(type);
    return type;
  }

  public String getProvidesType() {
    return ThymeleafMetadata.getMetadataIdentiferType();
  }

  public ControllerMVCService getControllerMVCService() {
    if (controllerMVCService == null) {
      // Get all Services implement ControllerMVCService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ControllerMVCService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          controllerMVCService = (ControllerMVCService) this.context.getService(ref);
          return controllerMVCService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ControllerMVCService on ThymeleafMetadataProviderImpl.");
        return null;
      }
    } else {
      return controllerMVCService;
    }
  }
}
