package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TransferAccountResponse extends BaseModel<BaseModel> {
    private List<UserAccountsData> userAccountsData;
}

class UserAccountsData {
    private long id;
    private String accountNumber;
    private double balance;
    private String name;
    private String role;
    private List<String> transactions;
}
