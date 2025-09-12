package iteration_2;

import models.CreateAccountRequest;
import models.CreateUserRequest;
import models.DepositAccountRequest;
import models.LoginUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminLoginUserRequest;
import requests.CreateAccount;
import requests.CreateUser;
import requests.UserDepositAccount;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositAccount {
    String adminToken;
    String user1Token;
    String user2Token;
    //Генерация уникальных userName для каждого теста
    String user1Username = "A_" + System.currentTimeMillis(); // "kate001_123456"
    String user2Username = "B_" + System.currentTimeMillis(); // "kate002_123456"
    String user1Password = "Kate012!";
    String user2Password = "Kate013!";
    String userRole = "USER";
    static long firstAccountUser1;
    static int secondAccountUser1;
    static Random random = new Random();
    long accountId;
    double depositAmount;
    double initialBalance;

    DepositAccountRequest depositUser1Account = DepositAccountRequest.builder()
            .id(accountId)
            .balance(depositAmount)
            .build();

    DepositAccountRequest initialDepositUser1Account = DepositAccountRequest.builder()
            .id(accountId)
            .balance(initialBalance)
            .build();

    public long createAccount() {
        CreateAccountRequest user1Account = CreateAccountRequest.builder()
                .build();
        return new CreateAccount(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.entityWasCreated())
                .post(user1Account)
                .extract()
                .path("id");
    }

    @BeforeEach
    public void setupTestData() {
        //Создание первого пользователя
        CreateUserRequest createUser1Request = CreateUserRequest.builder()
                .username(user1Username)
                .password(user1Password)
                .role(userRole)
                .build();
        new CreateUser(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.entityWasCreated())
                .post(createUser1Request);
        //Получение токена для пользователя1
        LoginUserRequest loginUser1 = LoginUserRequest.builder()
                .username(user1Username)
                .password(user1Password)
                .build();

        user1Token = new AdminLoginUserRequest(RequestSpecs.unAuthSpec(), ResponseSpecs.requestReturnsOK())
                .post(loginUser1)
                .extract()
                .header("Authorization");

        //Создание счета первого пользователя
        firstAccountUser1 = createAccount();
    }

    public static Stream<Arguments> transactionDataForPositiveCaseJson() {

        return Stream.of(
                Arguments.of(firstAccountUser1, 0, 100, 100),
                Arguments.of(firstAccountUser1, 100, 200, 300),
                Arguments.of(firstAccountUser1, 300, 0.01, 300.01)
        );
    }

    public static Stream<Arguments> transactionDataForNegativeCase() {
        return Stream.of(
                //Пополнение на ноль
                Arguments.of(firstAccountUser1, 0, 0),
                Arguments.of(firstAccountUser1, 100, 0),
                //Пополнение на отрицательную сумму
                Arguments.of(firstAccountUser1, 0, -100),
                Arguments.of(firstAccountUser1, 200, -100)
        );
    }

    public static Stream<Arguments> notExistOrSomebodyAccount() {
        int randomIdAccount = new Random().nextInt(10000, 1000000);

        return Stream.of(
                Arguments.of(randomIdAccount, 100)  // ← передаем только ID
        );
    }


    @ParameterizedTest
    @DisplayName("Успешное пополнение счета первый и последующие разы")
    @MethodSource("transactionDataForPositiveCaseJson")
    public void userCanDepositAccountTest(long accountId, double initialBalance, double depositAmount, double expectedBalance) {
        // 1. Устанавливаем начальный баланс (если нужно)
        if (initialBalance != 0) {
            new UserDepositAccount(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.requestReturnsOK())
                    .post(initialDepositUser1Account);
        }

        // 2. Делаем депозит
        Float actualBalance = new UserDepositAccount(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.requestReturnsOK())
                .post(depositUser1Account)
                .extract()
                .path("balance");

        // 3. Проверяем итоговый баланс
        assertEquals((float) expectedBalance, actualBalance, 0.01f);
    }

    @ParameterizedTest
    @DisplayName("Пользователь не может пополнить счет на 0 и отрицательную сумму")
    @MethodSource("transactionDataForNegativeCase")
    public void userCannotDepositAccountTest(long accountId, double initialBalance, double depositAmount) {
        // 1. Устанавливаем начальный баланс (если нужно)
        if (initialBalance != 0) {
            new UserDepositAccount(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.requestReturnsOK())
                    .post(initialDepositUser1Account);
        }

        // 2. Делаем депозит
        new UserDepositAccount(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.requestReturnsBadRequest())
                .post(depositUser1Account);

    }

    @ParameterizedTest
    @DisplayName("Пользователь не может пополнить чужой или несуществующий счет")
    @MethodSource("notExistOrSomebodyAccount")
    public void userCannotDepositNotExistAccountTest(long accountId, double depositAmount) {
        new UserDepositAccount(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.requestReturnsForbidden())
                .post(depositUser1Account);
    }

}