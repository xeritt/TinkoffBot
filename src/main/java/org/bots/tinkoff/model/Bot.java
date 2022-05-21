package org.bots.tinkoff.model;

import org.bots.tinkoff.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToString;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@Component
public class Bot {
    static final Logger log = LoggerFactory.getLogger(Bot.class);
    public static final String NO_SELECT_LIST_ACCOUNT_FOR_TRADING = "NO SELECT(/list) ACCOUNT FOR TRADING!";
    public static final String NO_SELECT_DIRECTION = "NO SELECT DIRECTION FOR TRADING";
    public static int INITIAL_DELAY = 5;
    public static int PERIOD = 2;
    public static InvestApi api;
    private static String account;
    private static String figi;
    private static BotAnswer answer;
    //private static ScheduledFuture scheduled;
    private static ScheduledExecutorService scheduledExecutorService;
    private TelegrammBot telegrammBot;

    public TelegrammBot getTelegrammBot() {return telegrammBot;}
    public static InvestApi getApi() {return api;}
    public static String getFigi() {return figi;}
    public static String getAccount() {return account;}

    /*
            public Bot(String token, String figi) {
                if (Bot.apiTinkoff == null) {
                    apiTinkoff = InvestApi.createSandbox(token);
                    Bot.figi = figi;
                    log.info("Sand box mode = {}", apiTinkoff.isSandboxMode());
                }
            }
        */
    public void init(String token, String figi, TelegrammBot telebot) {
        if (Bot.api == null) {
            api = InvestApi.createSandbox(token);
            Bot.figi = figi;
            log.info("Sand box mode = {}", api.isSandboxMode());
        }
        this.telegrammBot = telebot;
    }

    public BotAnswer execCommands(String operation) {
        System.out.println(operation);
        StringTokenizer st = new StringTokenizer(operation, " ");
        System.out.println(st.countTokens());
        int count = st.countTokens();

        if (count == 0 ) return null;

        operation = st.nextToken();
        String param = "";
        System.out.println(operation);
        InvestApi api = Bot.api;

        if (count > 1 ) {
            param = st.nextToken();
            System.out.println(param);
            if (operation.equalsIgnoreCase("/list"))
                return listOrders(api, param);
            if (operation.equalsIgnoreCase("/stop"))
                return stopOrder(api, param);
            if (operation.equalsIgnoreCase("/figi")) {
                return setFigi(api, param);
            }
            if (operation.equalsIgnoreCase("/period")) {
                return setPeriod(api, param);
            }
        } else {
            if (operation.equalsIgnoreCase("/user")) {
                return usersService(api);
            }
            if (operation.equalsIgnoreCase("new-account")) {
                var acc = api.getSandboxService().openAccountSync();
                log.info("Account = {}", acc);
            }
            if (operation.equalsIgnoreCase("/buy")){
                OrderDirection dir = OrderDirection.ORDER_DIRECTION_BUY;
                return ordersService(api, figi, account, dir);
            }
            if (operation.equalsIgnoreCase("/sell")){
                OrderDirection dir = OrderDirection.ORDER_DIRECTION_SELL;
                return ordersService(api, figi, account, dir);
            }
            if (operation.equalsIgnoreCase("/price")){
                return getCandles(api, figi);
            }
            if (operation.equalsIgnoreCase("/del")){
                return delAccount(api, account);
            }
            if (operation.equalsIgnoreCase("/add")){
                return addAccount(api);
            }
            if (operation.equalsIgnoreCase("/start")){
                return startTradeService(api);
            }
            if (operation.equalsIgnoreCase("/done")){
                return stopTradeService(api);
            }
        }

        return help(api);
    }

    private BotAnswer setPeriod(InvestApi api, String param) {
        answer = new BotAnswer();
        try {
            PERIOD = Integer.parseInt(param);
            answer.append("Set trading period, sec = " + param);
        }catch (NumberFormatException e) {
            answer.append("Error! Number format please [5 or 10])");
        }
        return  answer;
    }

    public BotAnswer help(InvestApi api){
        BotAnswer answer = new BotAnswer();
        answer.addBtm("/add");
        answer.addBtm("/user");
        answer.addBtm("/price");
     //   answer.addBtm("/figi");
        answer.addBtm("/start");
        answer.addBtm("/done");
        answer.append("Commands /help /add /user /price /start /done /figi [name] /period [sec]");
        return  answer;
    }

    public BotAnswer addAccount(InvestApi api) {
        String res = api.getSandboxService().openAccountSync();
        BotAnswer answer = help(api);
        answer.setMessage(new StringBuilder());
        answer.append("Add new account :" + res);
        return answer;
    }

    public BotAnswer delAccount(InvestApi api, String account) {
        api.getSandboxService().closeAccountSync(account);
        BotAnswer answer = help(api);
        answer.setMessage(new StringBuilder());
        answer.append("Del account :" + account);
        return answer;
    }

    public BigDecimal getPrice(Quotation quotation){
        BigDecimal price = quotationToBigDecimal(quotation);
        return price;
    }

    public String printCandle(HistoricCandle candle) {

        BigDecimal open = quotationToBigDecimal(candle.getOpen());
        BigDecimal close = quotationToBigDecimal(candle.getClose());
        BigDecimal high = quotationToBigDecimal(candle.getHigh());
        BigDecimal low = quotationToBigDecimal(candle.getLow());
        long volume = candle.getVolume();
        String time = timestampToString(candle.getTime());
        //String str = String.format("{O: %d, C: %d, Min: %d, Max: %d, V: %d, T: %s}",
        String str = "[" + time + "]\n O:"+ open + " C:" + close + "\n Min:" + low + " Max:" + high + " \n V:" + volume ;
        log.info(
                "цена открытия: {}, цена закрытия: {}, минимальная цена за 1 лот: {}, максимальная цена за 1 лот: {}, объем " +
                        "торгов в лотах: {}, время свечи: {}",
                open, close, low, high, volume, time);
        return str;
    }

    public void showCandlesDay(){
        List<HistoricCandle> candlesHour = getHistoricCandles(CandleInterval.CANDLE_INTERVAL_HOUR);

        //BotAnswer answer = new BotAnswer();
        log.info("получено {} 1-минутных свечей для инструмента с figi {}", candlesHour.size(), figi);

        answer.append("получено " + candlesHour.size() + " 1-часовых свечей для \n figi: " + figi);
        answer.append("\n");

        for (HistoricCandle candle : candlesHour) {
            answer.append(printCandle(candle));
            answer.append("\n");
        }
        sendMessage(answer.getText());
    }


    public List<HistoricCandle> getHistoricCandles(CandleInterval interval) {
        List<HistoricCandle> candlesHour = api.getMarketDataService()
                .getCandlesSync(figi,
                        Instant.now().minus(1, ChronoUnit.DAYS),
                        Instant.now(),
                        interval
                );
        return candlesHour;
    }

    public void sendMessage(String text){
        String chatId = getTelegrammBot().getChatId();
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            getTelegrammBot().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public BotAnswer getCandles(InvestApi api, String figi) {

        //Получаем и печатаем список свечей для инструмента
        //var figi = randomFigi(api, 1).get(0);
        /*
        List<HistoricCandle> candles1min = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                        CandleInterval.CANDLE_INTERVAL_1_MIN);

        List<HistoricCandle> candles5min = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                        CandleInterval.CANDLE_INTERVAL_5_MIN);
        List<HistoricCandle> candles15min = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                        CandleInterval.CANDLE_INTERVAL_15_MIN);
        */
        List<HistoricCandle> candlesHour = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                        CandleInterval.CANDLE_INTERVAL_HOUR);
        /*
        List<HistoricCandle> candlesDay = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(), CandleInterval.CANDLE_INTERVAL_DAY);
        */
        BotAnswer answer = new BotAnswer();
        log.info("получено {} 1-минутных свечей для инструмента с figi {}", candlesHour.size(), figi);
        answer.append("получено " + candlesHour.size() + " 1-часовых свечей для \n figi: " + figi);
        answer.append("\n");

        for (HistoricCandle candle : candlesHour) {
            answer.append(printCandle(candle));
            answer.append("\n");
        }
/*
        log.info("получено {} 5-минутных свечей для инструмента с figi {}", candles5min.size(), figi);
        for (HistoricCandle candle : candles5min) {
            printCandle(candle);
        }

        log.info("получено {} 15-минутных свечей для инструмента с figi {}", candles15min.size(), figi);
        for (HistoricCandle candle : candles15min) {
            printCandle(candle);
        }

        log.info("получено {} 1-часовых свечей для инструмента с figi {}", candlesHour.size(), figi);
        for (HistoricCandle candle : candlesHour) {
            printCandle(candle);
        }

        log.info("получено {} 1-дневных свечей для инструмента с figi {}", candlesDay.size(), figi);
        for (HistoricCandle candle : candlesDay) {
            printCandle(candle);
        }

 */
        return answer;
    }

    public List<String> randomFigi(InvestApi api, int count) {
        return api.getInstrumentsService().getTradableSharesSync()
                .stream()
                .filter(el -> Boolean.TRUE.equals(el.getApiTradeAvailableFlag()))
                .map(Share::getFigi)
                .limit(count)
                .collect(Collectors.toList());
    }

    public BotAnswer ordersService(InvestApi api, String figi, String mainAccount, OrderDirection direction) {
        //Выставляем заявку
        BotAnswer answer = new BotAnswer();

        if (mainAccount == null) {
            answer.append(NO_SELECT_LIST_ACCOUNT_FOR_TRADING);
            return answer;
        }
        if (direction == null) {
            answer.append(NO_SELECT_DIRECTION);
            return answer;
        }
        //var accounts = api.getUserService().getAccountsSync();
        SandboxService service = api.getSandboxService();
        //var orderService = api.getSandboxService().pos
        //var accounts =  service.getAccountsSync();
        //var mainAccount = accounts.get(0).getId();
        Quotation lastPrice = api.getMarketDataService().getLastPricesSync(List.of(figi)).get(0).getPrice();
        log.info("lastPrice = {}", lastPrice);
        answer.append("lastPrice = " + lastPrice);
        answer.append("\n");

        Quotation minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
        log.info("minPriceIncrement = {}", minPriceIncrement);
        answer.append("minPriceIncrement = " + minPriceIncrement);
        answer.append("\n");

        Quotation price = Quotation.newBuilder().setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 10)
                .setNano(lastPrice.getNano() - minPriceIncrement.getNano() * 100).build();
        log.info("price = {}", price);
        answer.append("price = " + price);
        answer.append("\n");

        //Выставляем заявку на покупку по лимитной цене
        String orderId = service//api.getOrdersService()
                .postOrderSync(
                        figi,
                        1,
                        price,
                        direction,
                        mainAccount,
                        OrderType.ORDER_TYPE_LIMIT,
                        UUID.randomUUID().toString()
                ).getOrderId();
        log.info("OrderId = {}", orderId);
        answer.appendln("OrderId = " + orderId);
        return answer;
    }

    public BotAnswer stopOrder(InvestApi api, String orderId){
        BotAnswer answer = new BotAnswer();
        SandboxService service = getService(api);
        //String mainAccount = getAccount(service);
        service.cancelOrder(account, orderId);
        answer.append("Order " + orderId + " cancel");
        return answer;
    }

    public SandboxService getService(InvestApi api){
        return api.getSandboxService();
    }

    /*
    public static String getAccount(SandboxService service) {
        List<Account> accounts =  service.getAccountsSync();
        String mainAccount = accounts.get(0).getId();
        return mainAccount;
    }*/

    public List<OrderState> getOrderState(InvestApi api, String mainAccount){
        SandboxService service = getService(api);
        List<OrderState> orders = service.getOrdersSync(mainAccount);
        return  orders;
    }

    public BotAnswer listOrders(InvestApi api, String mainAccount){
        account = mainAccount;
        SandboxService service = getService(api);
        BotAnswer answer = new BotAnswer();

        log.info("Account = {}", mainAccount);
        answer.append("Account = " + mainAccount);
        answer.append("\n");
        //Получаем список активных заявок, проверяем наличие нашей заявки в списке
        List<OrderState>  orders = service.getOrdersSync(mainAccount);
        log.info("Count orders: " + orders.size());
        answer.append("Count orders: " + orders.size());
        answer.append("\n");

        for (int i = 0; i < orders.size(); i++) {
            OrderState order = orders.get(i);
            String figi = order.getFigi();
            String orderId = order.getOrderId();
            String dir = order.getDirection().name();
            log.info("Figi = {}, OrderId = {}, dir = {}, ", figi , orderId, dir);
            //answer.appendln("Figi = " + figi + "\n OrderId = " + orderId + "\n dir = " + dir );
            /*answer.appendln("Currency = " + order.getCurrency());
            answer.appendln("Average position = " + order.getAveragePositionPrice());
            answer.appendln("Init price = " + order.getInitialOrderPrice());
            answer.appendln("Total amount = " + order.getTotalOrderAmount());*/
            answer.appendln(order.toString());
            answer.appendln("Init price = " + order.getInitialOrderPrice());
            answer.addBtm("/stop "+ orderId);
        }
        answer.addBtm("/sell");
        answer.addBtm("/buy");
        if (orders.size() == 0) {
            answer.addBtm("/del");
        }
        answer.addBtm("/help");
        return answer;
    }
    public BotAnswer instrumentsService(InvestApi api) {
        List<Bond> bonds = api.getInstrumentsService().getTradableBondsSync();
        List<Future> futures = api.getInstrumentsService().getTradableFuturesSync();
        BotAnswer answer = new BotAnswer();
        log.info("Список Bonds");
        answer.append("Список Bonds");
        for (int i = 0; i < bonds.size(); i++) {
            Bond bond = bonds.get(i);
            String figi = bond.getFigi();
            log.info("figi {} code {} currency {}", figi, bond.getClassCode(), bond.getCurrency());
            answer.append(figi + " class=" + bond.getClassCode() + " cur=" +  bond.getCurrency());
            answer.append("\n");
        }
        /*
        log.info("Список Futures");
        for (int i = 0; i < futures.size(); i++) {
            Future future = futures.get(i);
            String figi = future.getFigi();
            log.info("figi {} code {} currency {}", figi, future.getClassCode(), future.getCurrency());
        }*/
        return answer;
    }

    public BotAnswer usersService(InvestApi api) {
        SandboxService service = getService(api);
        List<Account> accounts = service.getAccountsSync();
        BotAnswer answer = new BotAnswer();
        answer.appendln("figi = " + figi);
        if (accounts.size() <= 0 ) {
            answer.append("No accounts");
            return answer;
        }
        Account mainAccount = accounts.get(0);
        for (Account account : accounts) {
            String id = account.getId();
            String name = account.getAccessLevel().name();
            log.info("account id: {}, access level: {}", id, name);
            answer.append("account id " + id + '\n'+"access level: "+ name + '\n');
            answer.addBtm("/list " + id);
        }

        if (Bot.getAccount() == null){
            answer.appendln(NO_SELECT_LIST_ACCOUNT_FOR_TRADING);
        }
        answer.appendln("Trading period, sec = " + Bot.PERIOD);
        answer.addBtm("/help");
        if (api.isSandboxMode()) return answer;
        //Получаем и печатаем информацию о текущих лимитах пользователя

        GetUserTariffResponse tariff = api.getUserService().getUserTariffSync();
        log.info("stream type: marketdata, stream limit: {}", tariff.getStreamLimitsList().get(0).getLimit());
        log.info("stream type: orders, stream limit: {}", tariff.getStreamLimitsList().get(1).getLimit());
        log.info("current unary limit per minute: {}", tariff.getUnaryLimitsList().get(0).getLimitPerMinute());

        //Получаем и печатаем информацию об обеспеченности портфеля
        GetMarginAttributesResponse marginAttributes = api.getUserService().getMarginAttributesSync(mainAccount.getId());
        log.info("Ликвидная стоимость портфеля: {}", moneyValueToBigDecimal(marginAttributes.getLiquidPortfolio()));
        log.info("Начальная маржа — начальное обеспечение для совершения новой сделки: {}",
                moneyValueToBigDecimal(marginAttributes.getStartingMargin()));
        log.info("Минимальная маржа — это минимальное обеспечение для поддержания позиции, которую вы уже открыли: {}",
                moneyValueToBigDecimal(marginAttributes.getMinimalMargin()));
        log.info("Уровень достаточности средств. Соотношение стоимости ликвидного портфеля к начальной марже: {}",
                quotationToBigDecimal(marginAttributes.getFundsSufficiencyLevel()));
        log.info("Объем недостающих средств. Разница между стартовой маржой и ликвидной стоимости портфеля: {}",
                moneyValueToBigDecimal(marginAttributes.getAmountOfMissingFunds()));
        return answer;
    }

    public BotAnswer startTradeService(InvestApi api){
        answer = new BotAnswer();
        TradeService trade = new TradeService(this);
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newScheduledThreadPool(5);
            ScheduledFuture scheduled =
                    scheduledExecutorService.scheduleAtFixedRate(
                            trade, INITIAL_DELAY, PERIOD, TimeUnit.SECONDS);
            answer.appendln("Start Trade Service");
        } else {
            answer.appendln("Trade Service all ready run");
        }
        return answer;
    }

    public BotAnswer stopTradeService(InvestApi api){
        answer = new BotAnswer();
        scheduledExecutorService.shutdown();
        scheduledExecutorService = null;
        answer.appendln("Stop Trade Service");
        return answer;
    }

    public BotAnswer setFigi(InvestApi api, String figi){
        Bot.figi = figi;
        answer = new BotAnswer("Change figi to " + figi);
        return answer;
    }
}
