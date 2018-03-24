package org.wordpress.android.e2etests.utils;



public class TestDataExample {
    private String[] mUITestsData = {
            "example@email.com", // email
            "passwordhere", // password
    };


    public String getEmail() {
        String email;

        email = mUITestsData[0];

        return email;
    }

    public String getPassword() {
        String password;

        password = mUITestsData[1];

        return password;
    }
}
