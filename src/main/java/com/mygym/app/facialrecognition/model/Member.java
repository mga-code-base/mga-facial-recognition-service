package com.mygym.app.facialrecognition.model;

public class Member {
	
    private String memberId;

    private String name;

    private Long mobileNumber;

    private String emailAddress;

    private String membershipStartDate;
    
    private String membershipEndDate;

    private Boolean isPrivilegedMember;

    private Integer localityPincode;

    private String photo;

    private String username;

    private String password;

    private String trainerAssigned;

    private String addedByAdminId;
    
    private Boolean isActive = false;
    
    private Boolean isFacialRegCompleted = false;

    public Member() {}
    public Member(String invalidId) {
    	this.memberId = invalidId;
    }
    public Member(String memberId, String name, Long mobileNumber, String emailAddress, String membershipStartDate, String membershipEndDate, Boolean isPrivilegedMember, Integer localityPincode, String photo, String username, String password, String trainerAssigned, String addedByAdminId, Boolean isActive, Boolean isFacialRegCompleted) {
    	this.memberId = memberId;
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.emailAddress = emailAddress;
        this.membershipStartDate = membershipStartDate;
        this.isPrivilegedMember = isPrivilegedMember;
        this.localityPincode = localityPincode;
        this.photo = photo;
        this.username = username;
        this.password = password;
        this.trainerAssigned = trainerAssigned;
        this.addedByAdminId = addedByAdminId;
        this.isActive = isActive;
        this.isFacialRegCompleted = isFacialRegCompleted;
    }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(Long mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getMembershipStartDate() { return membershipStartDate; }
    public void setMembershipStartDate(String membershipStartDate) { this.membershipStartDate = membershipStartDate; }

    public String getMembershipEndDate() { return membershipEndDate; }
	public void setMembershipEndDate(String membershipEndDate) { this.membershipEndDate = membershipEndDate; }

	public Boolean getIsPrivilegedMember() { return isPrivilegedMember; }
    public void setIsPrivilegedMember(Boolean isPrivilegedMember) { this.isPrivilegedMember = isPrivilegedMember; }

    public Integer getLocalityPincode() { return localityPincode; }
    public void setLocalityPincode(Integer localityPincode) { this.localityPincode = localityPincode; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTrainerAssigned() { return trainerAssigned; }
    public void setTrainerAssigned(String trainerAssigned) { this.trainerAssigned = trainerAssigned; }

    public String getAddedByAdminId() { return addedByAdminId; }
    public void setAddedByAdminId(String addedByAdminId) { this.addedByAdminId = addedByAdminId; }

	public Boolean getIsActive() { return isActive;	}
	public void setIsActive(Boolean isActive) { this.isActive = isActive;}

	public Boolean getIsFacialRegCompleted() { return isFacialRegCompleted;	}
	public void setIsFacialRegCompleted(Boolean isFacialRegCompleted) {	this.isFacialRegCompleted = isFacialRegCompleted;}
}
