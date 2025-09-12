package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepositAccountResponse <T extends Number> extends BaseModel<BaseModel> {
    private long id;
    private String accountNumber;
    private T balance;
    private List<String> transactions;

}
