package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferAccountRequest extends BaseModel<BaseModel> {
    private long senderAccountId;
    private long receiverAccountId;
    private double amount;
}
