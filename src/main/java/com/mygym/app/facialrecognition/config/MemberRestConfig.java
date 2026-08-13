package com.mygym.app.facialrecognition.config;

import static com.mygym.app.facialrecognition.util.FacialConstants.MEMBER_REST_CLIENT_BEAN;
import static com.mygym.app.facialrecognition.util.FacialConstants.MEMBER_CRUD_BASE_URL;
import static com.mygym.app.facialrecognition.util.FacialConstants.CONTENT_HEADER;
import static com.mygym.app.facialrecognition.util.FacialConstants.JSON_HEADER;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class MemberRestConfig {

    private final ClientHttpRequestInterceptor loadBalancerInterceptor;

    public MemberRestConfig(@Qualifier("loadBalancerInterceptor") ObjectProvider<ClientHttpRequestInterceptor> loadBalancerInterceptorProvider) {
        this.loadBalancerInterceptor = loadBalancerInterceptorProvider.getIfAvailable();
    }
	
    @Bean(name = MEMBER_REST_CLIENT_BEAN)
    public RestClient restCrudClient() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(MEMBER_CRUD_BASE_URL)
                .defaultHeader(CONTENT_HEADER, JSON_HEADER);
        if (loadBalancerInterceptor != null) builder.requestInterceptor(loadBalancerInterceptor);
        return builder.build();
    }
}
