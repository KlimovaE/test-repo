package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CreateAccountResponse extends BaseModel<BaseModel> {
    private long id;
    private String accountNumber;
    private String balance;
    private List<String> transactions;

}
