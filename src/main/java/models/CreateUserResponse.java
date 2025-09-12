package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserResponse {
        private long id;
        private String username;
        private String password;
        private String  name;
        private String role;
        private List<String> accounts;
}
