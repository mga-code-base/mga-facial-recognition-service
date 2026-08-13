package com.mygym.app.facialrecognition.util;

public class FacialConstants {

	public static final String MEMBER_REST_CLIENT_BEAN = "memberCrudClient";
	
	public static final String MEMBER_CRUD_BASE_URL = "http://mga-people-crud-service/crud/members/";
	
	public static final String CONTENT_HEADER = "Content-Type";
	
	public static final String JSON_HEADER = "application/json";
	
	public static final String FACIAL_REQUEST_MAPPING = "/api/face";
	
	public static final String FACIAL_REQUEST_MAPPING_SAFETY = "/api/face/";
	
	public static final String MEMBER_GET_BY_USERNAME_ENDPOINT = "/username/{username}";
	
	public static final String MEMBER_GET_BY_MEMBERID_ENDPOINT = "memberId";
	
	public static final String MEMBER_UPDATE_FACE_REGIS_ENDPOINT = "/faceregis/{memberId}";
}
