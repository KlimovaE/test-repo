package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepositAccountRequest <T extends Number> extends BaseModel<BaseModel> {
    private long id;
    private T balance;
}
