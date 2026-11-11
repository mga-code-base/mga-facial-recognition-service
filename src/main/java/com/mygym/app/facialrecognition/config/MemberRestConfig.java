package com.mygym.app.facialrecognition.config;

import static com.mygym.app.facialrecognition.util.FacialConstants.MEMBER_CRUD_PATH;
import static com.mygym.app.facialrecognition.util.FacialConstants.MEMBER_REST_CLIENT_BEAN;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.mygym.app.facialrecognition.client.DownstreamClients;

@Configuration
public class MemberRestConfig {

    @Bean(name = MEMBER_REST_CLIENT_BEAN)
    public RestClient restCrudClient(DownstreamClients clients, @Value("${mga.clients.people-crud-url}") String peopleCrudUrl) {
        return clients.create("people-crud", peopleCrudUrl + MEMBER_CRUD_PATH);
    }
}
