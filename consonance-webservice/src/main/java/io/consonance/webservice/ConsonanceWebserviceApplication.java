/*
 *     Consonance - workflow software for multiple clouds
 *     Copyright (C) 2016 OICR
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package io.consonance.webservice;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.Provision;
import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.jdbi.ConsonanceUserDAO;
import io.consonance.webservice.jdbi.JobDAO;
import io.consonance.webservice.jdbi.ProvisionDAO;
import io.consonance.webservice.resources.ConfigurationResource;
import io.consonance.webservice.resources.OrderResource;
import io.consonance.webservice.resources.TemplateHealthCheck;
import io.consonance.webservice.resources.UserResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthFactory;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthFactory;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

/**
 *
 * @author dyuen
 */
public class ConsonanceWebserviceApplication extends Application<ConsonanceWebserviceConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(ConsonanceWebserviceApplication.class);

    public static void main(String[] args) throws Exception {
        new ConsonanceWebserviceApplication().run(args);
    }

    private final HibernateBundle<ConsonanceWebserviceConfiguration> hibernate = new HibernateBundle<ConsonanceWebserviceConfiguration>(
            Job.class, Provision.class, ConsonanceUser.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(ConsonanceWebserviceConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    @Override
    public String getName() {
        return "webservice";
    }

    @Override
    public void initialize(Bootstrap<ConsonanceWebserviceConfiguration> bootstrap) {
        // setup swagger
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.2");
        beanConfig.setSchemes(new String[] { "http" });
        beanConfig.setHost("localhost:8080");
        beanConfig.setBasePath("/");
        beanConfig.setResourcePackage("io.consonance.webservice.resources,io.swagger.workflow.api,,io.swagger.task.api");
        beanConfig.setScan(true);
        beanConfig.setTitle("Swagger Consonance Prototype");

        // setup hibernate+postgres
        bootstrap.addBundle(hibernate);

        // serve static html as well
        bootstrap.addBundle(new AssetsBundle("/assets/", "/static/"));
        // enable views
        bootstrap.addBundle(new ViewBundle<>());

        // lookup environment variables (to get the home directory)

        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor()));
        
    }

    @Override
    public void run(ConsonanceWebserviceConfiguration configuration, Environment environment) {

        final TemplateHealthCheck healthCheck = new TemplateHealthCheck(configuration.getTemplate());
        environment.healthChecks().register("template", healthCheck);

        final JobDAO dao = new JobDAO(hibernate.getSessionFactory());
        final ProvisionDAO provisionDAO = new ProvisionDAO(hibernate.getSessionFactory());
        final ConsonanceUserDAO userDAO = new ConsonanceUserDAO(hibernate.getSessionFactory());

        environment.getObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        environment.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        environment.getObjectMapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        environment.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        environment.jersey().register(new OrderResource(dao, provisionDAO, configuration.getConsonanceConfig()));
        environment.jersey().register(new UserResource(userDAO));
        environment.jersey().register(new ConfigurationResource(configuration));

        // swagger stuff

        // Swagger providers
        environment.jersey().register(ApiListingResource.class);
        environment.jersey().register(SwaggerSerializers.class);
        LOG.info("This is our custom logger saying that we're about to load authenticators");
        // setup authentication
        SimpleJPAAuthenticator authenticator = new SimpleJPAAuthenticator(userDAO);
        CachingAuthenticator<String, ConsonanceUser> cachingAuthenticator = new CachingAuthenticator<String, ConsonanceUser>(
                environment.metrics(), authenticator, configuration.getAuthenticationCachePolicy());
        environment.jersey().register(
                AuthFactory.binder(new OAuthFactory<>(cachingAuthenticator, "SUPER SECRET STUFF", ConsonanceUser.class)));

        // optional CORS support
        // Enable CORS headers
        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
                "X-Requested-With,Content-Type,Accept,Content-Length,Origin,api_key,Authorization");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
        cors.setInitParameter(CrossOriginFilter.EXPOSED_HEADERS_PARAM, "true");


        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }
}
