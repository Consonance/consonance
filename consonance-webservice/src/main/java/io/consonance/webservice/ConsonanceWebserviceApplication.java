/*
 * Copyright (C) 2015 Consonance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.consonance.webservice;

import io.consonance.webservice.core.WorkflowRun;
import io.consonance.webservice.jdbi.WorkflowRunDAO;
import io.consonance.webservice.resources.TemplateHealthCheck;
import io.consonance.webservice.resources.WorkflowRunResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import org.apache.http.client.HttpClient;
import org.eclipse.jetty.servlets.CrossOriginFilter;

/**
 *
 * @author dyuen
 */
public class ConsonanceWebserviceApplication extends Application<ConsonanceWebserviceConfiguration> {

    public static void main(String[] args) throws Exception {
        new ConsonanceWebserviceApplication().run(args);
    }

    private final HibernateBundle<ConsonanceWebserviceConfiguration> hibernate = new HibernateBundle<ConsonanceWebserviceConfiguration>(
            WorkflowRun.class) {
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
        beanConfig.setResourcePackage("io.consonance.webservice.resources");
        beanConfig.setScan(true);
        beanConfig.setTitle("Swagger Consonance Prototype");

        // setup hibernate+postgres
        bootstrap.addBundle(hibernate);

        // serve static html as well
        bootstrap.addBundle(new AssetsBundle("/assets/", "/static/"));
        // enable views
        bootstrap.addBundle(new ViewBundle<ConsonanceWebserviceConfiguration>());
    }

    @Override
    public void run(ConsonanceWebserviceConfiguration configuration, Environment environment) {

        final TemplateHealthCheck healthCheck = new TemplateHealthCheck(configuration.getTemplate());
        environment.healthChecks().register("template", healthCheck);

        final WorkflowRunDAO dao = new WorkflowRunDAO(hibernate.getSessionFactory());
        final HttpClient httpClient = new HttpClientBuilder(environment).using(configuration.getHttpClientConfiguration()).build(getName());

        environment.jersey().register(new WorkflowRunResource(dao));

        // swagger stuff

        // Swagger providers
        environment.jersey().register(ApiListingResource.class);
        environment.jersey().register(SwaggerSerializers.class);

        // optional CORS support
        // Enable CORS headers
        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }
}
