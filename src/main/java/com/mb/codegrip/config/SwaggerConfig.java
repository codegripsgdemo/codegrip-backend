package com.mb.codegrip.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMethod;

import com.mb.codegrip.constants.CodeGripConstants;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.AuthorizationCodeGrantBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.service.TokenEndpoint;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:messages.properties"), @PropertySource("classpath:profiles/${spring.profiles.active}/application.properties"),
		@PropertySource("classpath:swagger.properties") })
public class SwaggerConfig {

	@Value("${app.client.id}")
	private String clientId;
	
	@Value("${app.client.secret}")
	private String clientSecret;

	@Value("${app.client.password}")
	private String clientPassword;

	@Value("${host.full.dns.auth.link}")
	private String authLink;
	@Autowired
	private Environment environment;

	@Bean
	public Docket api() {

		List<ResponseMessage> list = new java.util.ArrayList<>();
		list.add(new ResponseMessageBuilder().code(500).message(environment.getProperty(CodeGripConstants.SERVER_ERROR))
				.responseModel(new ModelRef(CodeGripConstants.RESULT)).build());
		list.add(new ResponseMessageBuilder().code(401).message(environment.getProperty(CodeGripConstants.UNAUTHORIZED))
				.responseModel(new ModelRef(CodeGripConstants.RESULT)).build());
		list.add(new ResponseMessageBuilder().code(406)
				.message(environment.getProperty(CodeGripConstants.NOT_ACCEPTABLE))
				.responseModel(new ModelRef(CodeGripConstants.RESULT)).build());
		list.add(new ResponseMessageBuilder().code(200).message(environment.getProperty(CodeGripConstants.SUCCESS))
				.responseModel(new ModelRef(CodeGripConstants.RESULT)).build());
		list.add(new ResponseMessageBuilder().code(404)
				.message(environment.getProperty(CodeGripConstants.RESOURCE_NOT_FOUND))
				.responseModel(new ModelRef(CodeGripConstants.RESULT)).build());
		list.add(new ResponseMessageBuilder().code(201).message(environment.getProperty(CodeGripConstants.CREATED))
				.responseModel(new ModelRef(CodeGripConstants.RESULT)).build());
		list.add(new ResponseMessageBuilder().code(415)
				.message(environment.getProperty(CodeGripConstants.UNSUPPORTED_TYPE))
				.responseModel(new ModelRef(CodeGripConstants.RESULT)).build());

		return new Docket(DocumentationType.SWAGGER_2).select()
				.apis(RequestHandlerSelectors
						.basePackage(environment.getProperty(CodeGripConstants.SWAGGER_BASE_PACKAGE)))
				.paths(PathSelectors.any()).build().consumes(getAllConsumeContentTypes())
				.securitySchemes(Arrays.asList(securitySchema()))
				.securityContexts(Collections.singletonList(securityContext())).pathMapping("/")
				.useDefaultResponseMessages(false).apiInfo(apiInfo()).globalResponseMessage(RequestMethod.GET, list)
				.globalResponseMessage(RequestMethod.POST, list);

	}

	private Set<String> getAllConsumeContentTypes() {
		Set<String> consumes = new HashSet<>();
		consumes.add(CodeGripConstants.APPLICATION_X_WWW_FORM_URLENCODED);
		consumes.add(CodeGripConstants.APPLICATION_JSON);
		return consumes;
	}

	private SecurityScheme securitySchema() {
		GrantType grantType = new AuthorizationCodeGrantBuilder()
				.tokenEndpoint(new TokenEndpoint(environment.getProperty(CodeGripConstants.OAUTH_URL), clientPassword))
				.build();

		return new OAuthBuilder().name(CodeGripConstants.SPRING_OAUTH)
				.grantTypes(Arrays.asList(grantType)).scopes(Arrays.asList(scopes())).build();

	}

	private AuthorizationScope[] scopes() {
		return new AuthorizationScope[] { new AuthorizationScope(CodeGripConstants.READ, CodeGripConstants.READ_DETAILS),
				new AuthorizationScope(CodeGripConstants.WRITE, CodeGripConstants.WRITE_DETAILS) };
	}

	private SecurityContext securityContext() {
		return SecurityContext.builder().securityReferences(defaultAuth()).forPaths(PathSelectors.ant("/user/**"))
				.build();
	}

	private List<SecurityReference> defaultAuth() {

		final AuthorizationScope[] authorizationScopes = new AuthorizationScope[3];
		authorizationScopes[0] = new AuthorizationScope("read", "read all");
		authorizationScopes[1] = new AuthorizationScope("trust", "trust all");
		authorizationScopes[2] = new AuthorizationScope("write", "write all");

		return Collections.singletonList(new SecurityReference("oauth2schema", authorizationScopes));
	}

	@SuppressWarnings("deprecation")
	@Bean
	public SecurityConfiguration securityInfo() {
		return new SecurityConfiguration(clientId, clientSecret, "", "", "", ApiKeyVehicle.HEADER,
				CodeGripConstants.AUTHORIZATION, ": Basic ");
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title(environment.getProperty(CodeGripConstants.SWAGGER_TITLE))
				.description(environment.getProperty(CodeGripConstants.SWAGGER_DESCRIPTION))
				.termsOfServiceUrl(environment.getProperty(CodeGripConstants.SWAGGER_TERMSOFSERVICE))
				.contact(new Contact(environment.getProperty(CodeGripConstants.SWAGGER_DEVELOPER_CONTACT_NAME),
						environment.getProperty(CodeGripConstants.SWAGGER_TERMSOFSERVICE),
						environment.getProperty(CodeGripConstants.SWAGGER_CONTACT_EMAIL)))
				.license(CodeGripConstants.SWAGGER_LICENSE_TYPE)
				.licenseUrl(environment.getProperty(CodeGripConstants.SWAGGER_LICENSE_URL))
				.version(environment.getProperty(CodeGripConstants.SWAGGER_VERSION)).build();
	}
}