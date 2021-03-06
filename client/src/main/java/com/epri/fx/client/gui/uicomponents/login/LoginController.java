package com.epri.fx.client.gui.uicomponents.login;

import com.epri.fx.client.bean.MenuVoCell;
import com.epri.fx.client.gui.uicomponents.main.MainController;
import com.epri.fx.client.request.Request;
import com.epri.fx.client.request.feign.admin.MenuFeign;
import com.epri.fx.client.request.feign.login.LoginFeign;
import com.epri.fx.client.store.ApplicatonStore;
import com.epri.fx.client.utils.AlertUtil;
import com.epri.fx.client.websocket.Session;
import com.epri.fx.server.util.EncryptUtil;
import com.epri.fx.server.util.user.JwtAuthenticationRequest;
import com.epri.fx.server.vo.FrontUser;
import com.epri.fx.server.vo.MenuVO;
import com.epri.fx.server.vo.PermissionInfo;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.svg.SVGGlyph;
import com.jfoenix.svg.SVGGlyphLoader;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.flow.context.ViewFlowContext;
import io.datafx.controller.util.VetoException;
import io.datafx.core.concurrent.ProcessChain;
import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description:
 * @className: ConversationItemPresenter
 * @author: liwen
 * @date: 2019-09-25 16:51
 */
@ViewController("/fxml/login/login.fxml")
public class LoginController {

    @FXML
    private StackPane rootPane;
    @FXML
    private Pane imagePane;

    @FXML
    private StackPane centerPane;

    //正面视图
    @FXML
    public HBox loginPane;
    //反面视图
    @FXML
    public HBox registeredPane;
    @FXML
    public Hyperlink registeredLink;
    @FXML
    public Hyperlink loginLink;
    @FXML
    private Label errorLabel;
    @FXML
    private Label userIcon;
    @FXML
    private Label pwdIcon;
    @FXML
    private Label reuserIcon;
    @FXML
    private Label repwdIcon;
    @FXML
    private Label repwd2Icon;
    @FXML
    private JFXProgressBar lodingBar;
    @FXML
    private JFXTextField userNameTextField;
    @FXML
    private JFXPasswordField passWordTextField;
    //翻转角度
    private DoubleProperty angleProperty = new SimpleDoubleProperty(Math.PI / 2);
    //正面翻转特效
    private PerspectiveTransform frontEffect = new PerspectiveTransform();
    //反面翻转特效
    private PerspectiveTransform backEffect = new PerspectiveTransform();
    private Timeline frontTimeLine = new Timeline();
    private Timeline backTimeLine = new Timeline();

    @ActionHandler
    private FlowActionHandler actionHandler;

    private SequentialTransition sequentialTransition = new SequentialTransition();

    private DoubleProperty imageWidth = new SimpleDoubleProperty();
    private DoubleProperty imageHeiht = new SimpleDoubleProperty();


    @Inject
    private Session session;

    @FXML
    @ActionTrigger("login")
    private JFXButton loginBut;

    @FXMLViewFlowContext
    private ViewFlowContext flowContext;

    @PostConstruct
    public void init() {

        try {
            SVGGlyph userSvg = SVGGlyphLoader.getIcoMoonGlyph(ApplicatonStore.ICON_FONT_KEY + ".user-name");
            SVGGlyph pwdSvg = SVGGlyphLoader.getIcoMoonGlyph(ApplicatonStore.ICON_FONT_KEY + ".password");
            SVGGlyph reuserSvg = SVGGlyphLoader.getIcoMoonGlyph(ApplicatonStore.ICON_FONT_KEY + ".user-name");
            SVGGlyph repwdSvg = SVGGlyphLoader.getIcoMoonGlyph(ApplicatonStore.ICON_FONT_KEY + ".password");
            SVGGlyph repwd2Svg = SVGGlyphLoader.getIcoMoonGlyph(ApplicatonStore.ICON_FONT_KEY + ".querenmima");
            userSvg.setId("login-svg-glyph");
            pwdSvg.setId("login-svg-glyph");
            reuserSvg.setId("login-svg-glyph");
            repwdSvg.setId("login-svg-glyph");
            repwd2Svg.setId("login-svg-glyph");

            userIcon.setGraphic(userSvg);
            pwdIcon.setGraphic(pwdSvg);
            reuserIcon.setGraphic(reuserSvg);
            repwdIcon.setGraphic(repwdSvg);
            repwd2Icon.setGraphic(repwd2Svg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        imagePane.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                imageWidth.setValue(newValue);
            }
        });
        imagePane.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                imageHeiht.setValue(newValue);
            }
        });

        registeredPane.visibleProperty().bind(
                Bindings.when(angleProperty.lessThan(0)).then(true).otherwise(false));
        loginPane.visibleProperty().bind(registeredPane.visibleProperty().not());
        loginPane.managedProperty().bind(loginPane.visibleProperty());
        registeredPane.managedProperty().bind(registeredPane.visibleProperty());

        initAnimation();
        loadingImage();
        initAction();

    }

    private void initAnimation() {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), centerPane);
        fadeTransition.setFromValue(0f);
        fadeTransition.setToValue(1f);

        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(500), centerPane);
        translateTransition.setInterpolator(Interpolator.EASE_BOTH);
        translateTransition.setFromY(400);
        translateTransition.setToY(centerPane.getLayoutY());

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.setDelay(Duration.millis(1500));
        parallelTransition.getChildren().addAll(
                fadeTransition,
                translateTransition
        );
        parallelTransition.setCycleCount(1);
        parallelTransition.play();


        KeyFrame frame1 = new KeyFrame(Duration.ZERO, new KeyValue(angleProperty,
                Math.PI / 2, Interpolator.LINEAR));
        KeyFrame frame2 = new KeyFrame(Duration.seconds(0.7),
                new EventHandler() {
                    @Override
                    public void handle(Event event) {
                        loginPane.setEffect(null);
                        registeredPane.setEffect(null);
                    }

                }, new KeyValue(angleProperty, -Math.PI / 2, Interpolator.LINEAR));

        KeyFrame frame3 = new KeyFrame(Duration.seconds(.7), new EventHandler() {
            @Override
            public void handle(Event event) {
                loginPane.setEffect(null);
                registeredPane.setEffect(null);
            }

        }, new KeyValue(angleProperty,
                Math.PI / 2, Interpolator.LINEAR));
        KeyFrame frame4 = new KeyFrame(Duration.ZERO, new KeyValue(angleProperty, -Math.PI / 2, Interpolator.LINEAR));


        frontTimeLine.getKeyFrames().addAll(frame1, frame2);
        backTimeLine.getKeyFrames().addAll(frame4, frame3);
    }

    private void setPT(PerspectiveTransform pt, double t) {
        double width = 650;
        double height = 450;
        double radius = width / 2;
        double back = height / 10;
        pt.setUlx(radius - Math.sin(t) * radius);
        pt.setUly(0 - Math.cos(t) * back);
        pt.setUrx(radius + Math.sin(t) * radius);
        pt.setUry(0 + Math.cos(t) * back);
        pt.setLrx(radius + Math.sin(t) * radius);
        pt.setLry(height - Math.cos(t) * back);
        pt.setLlx(radius - Math.sin(t) * radius);
        pt.setLly(height + Math.cos(t) * back);
    }

    private void loadingImage() {

        ImageView logBack = new ImageView("/images/loginBack.jpg");
        logBack.fitHeightProperty().bind(imageHeiht);
        logBack.fitWidthProperty().bind(imageWidth);
        imagePane.getChildren().add(new Label("", logBack));

        List<File> files = Arrays.asList(new File(this.getClass().getResource("/images/login/").getPath()).listFiles());

        sequentialTransition.setAutoReverse(true);
        sequentialTransition.setCycleCount(Timeline.INDEFINITE);

        ProcessChain.create().addPublishingTask(() -> imagePane.getChildren(), p -> {
            for (int i = 0; i < files.size(); i++) {

                File file = files.get(i);
                if (!file.isDirectory()) {
                    String url = "/images/login/" + file.getName();
                    ImageView imageView = new ImageView(url);
                    imageView.fitHeightProperty().bind(imageHeiht);
                    imageView.fitWidthProperty().bind(imageWidth);
                    Label label = new Label("", imageView);

                    label.setOpacity(0d);
                    FadeTransition fadeT = new FadeTransition(Duration.millis(500), label);
                    fadeT.setDelay(Duration.millis(1500));
                    fadeT.setFromValue(0f);
                    fadeT.setToValue(1f);
                    fadeT.setCycleCount(1);
                    sequentialTransition.getChildren().add(fadeT);

                    p.publish(label);

                }

            }
        }).withFinal(() -> sequentialTransition.play()).run();

    }

    private void initAction() {
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        lodingBar.visibleProperty().bind(centerPane.disableProperty());
        lodingBar.managedProperty().bind(lodingBar.visibleProperty());

        userNameTextField.focusedProperty().addListener((o, oldVal, newVal) -> {
            if (!newVal) {
                userNameTextField.validate();
            }
        });
        passWordTextField.focusedProperty().addListener((o, oldVal, newVal) -> {
            if (!newVal) {
                passWordTextField.validate();
            }
        });

        loginBut.disableProperty().bind(Bindings.or(
                userNameTextField.textProperty().isEqualTo(""),
                passWordTextField.textProperty().isEqualTo("")));

        rootPane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (loginBut.isDisable() == false) {
                    login();
                }
            }

        });
        angleProperty.addListener((observable, oldValue, newValue) -> {
            setPT(frontEffect, angleProperty.get());
            setPT(backEffect, angleProperty.get() - Math.PI);
        });
        registeredLink.setOnAction(event -> {
            loginPane.setEffect(frontEffect);
            registeredPane.setEffect(backEffect);
            frontTimeLine.play();
        });
        loginLink.setOnAction(event -> {
            loginPane.setEffect(frontEffect);
            registeredPane.setEffect(backEffect);
            backTimeLine.play();
        });
    }


    /**
     * @Description:登录
     * @param: []
     * @return: void
     * @auther: liwen
     * @date: 2020/11/6 9:56 上午
     */
    @ActionMethod("login")
    private void login() {

        JwtAuthenticationRequest jwtAuthenticationRequest = new JwtAuthenticationRequest();
        jwtAuthenticationRequest.setUsername(userNameTextField.getText());
        jwtAuthenticationRequest.setPassword(EncryptUtil.getInstance().Base64Encode(passWordTextField.getText()));
        ProcessChain.create()
                .addRunnableInPlatformThread(() -> {
                    centerPane.setDisable(true);
                    loginBut.setText("正在登录...");
                })
                .addSupplierInExecutor(() -> Request.connector(LoginFeign.class).login(jwtAuthenticationRequest))
                .addConsumerInPlatformThread(rel -> {

                    if (rel.getStatus() == 200) {
                        errorLabel.setText("");
                        ApplicatonStore.setToken(rel.getData());
                        loadApplicatonStore();
                    } else {
                        lodingBar.requestFocus();
                        errorLabel.setText(rel.getMessage());
                    }

                })
                .onException(e -> {
                    e.printStackTrace();
                    errorLabel.setText("无法连接服务器，请检查服务器是否启动。");
                    lodingBar.requestFocus();
                })
                .withFinal(() -> {
                    centerPane.setDisable(false);
                    loginBut.setText("登录");
                }).run();


    }

    public void loadApplicatonStore() {
        ProcessChain.create()
                .addRunnableInPlatformThread(() -> {
                    try {
                        actionHandler.navigate(LoadingController.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    ApplicatonStore.setName("");
                    ApplicatonStore.getAllMenu().clear();
                    ApplicatonStore.getMenus().clear();
                    ApplicatonStore.getElements().clear();
                    ApplicatonStore.getPermissionMenus().clear();
                    ApplicatonStore.getRoles().clear();
                })
                .addSupplierInExecutor(() -> Request.connector(MenuFeign.class).getMenuAll())
                .addConsumerInPlatformThread(rel -> ApplicatonStore.getAllMenu().addAll(rel))
                .addSupplierInExecutor(() ->
                        Request.connector(LoginFeign.class).getInfo(ApplicatonStore.getToken())
                )
                .addConsumerInPlatformThread(rel -> {
                    if (rel.getStatus() == 200) {
                        FrontUser frontUser = rel.getData();
                        ApplicatonStore.setName(frontUser.name);
                        ApplicatonStore.getMenus().addAll(frontUser.getMenus());
                        ApplicatonStore.getRoles().addAll(frontUser.getRoles());
                        ApplicatonStore.getElements().addAll(frontUser.getElements());
                        ApplicatonStore.setIntroduction(frontUser.getDescription());

                        for (PermissionInfo permissionInfo : frontUser.getElements()) {
                            ApplicatonStore.getFeatureMap().put(permissionInfo.getCode(), permissionInfo.getName());
                        }


                    } else {
                        AlertUtil.show(rel);
                    }
                })
                .addSupplierInExecutor(() ->
                        Request.connector(LoginFeign.class).getMenus(ApplicatonStore.getToken())
                )
                .addConsumerInPlatformThread(rel -> {
                    ApplicatonStore.getPermissionMenus().addAll(rel);

                })
                .addSupplierInExecutor(() -> {

                    List<MenuVO> allMenuList = ApplicatonStore.getAllMenu();
                    Map<Integer, List<MenuVO>> allMap = allMenuList.stream().collect(Collectors.groupingBy(MenuVO::getParentId));
                    MenuVO rootMenu = allMenuList.stream().min(Comparator.comparing(MenuVO::getParentId)).get();


                    List<MenuVO> permissionInfoList = ApplicatonStore.getPermissionMenus();
                    Map<Integer, List<MenuVO>> permissionInfoMap = permissionInfoList.stream().collect(Collectors.groupingBy(MenuVO::getParentId));
                    Map<String, List<MenuVO>> permissonTitleMap = permissionInfoList.stream().collect(Collectors.groupingBy(MenuVO::getTitle));

                    for (MenuVO menu : allMap.get(rootMenu.getId())) {

                        List<MenuVO> childrenMenus = permissionInfoMap.get(menu.getId());

                        List<MenuVO> partMenus = permissonTitleMap.get(menu.getTitle());
                        if (childrenMenus == null && partMenus == null) {

                            continue;
                        }

                        MenuVoCell menuVoCell = new MenuVoCell(menu, childrenMenus);

                        ApplicatonStore.getMenuVoCells().add(menuVoCell);

                    }
                    return 0;

                })
                .addConsumerInPlatformThread(rel -> {

                    try {
                        actionHandler.navigate(MainController.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .onException(e -> {
                    e.printStackTrace();
                    try {
                        actionHandler.navigate(LoginController.class);
                    } catch (VetoException vetoException) {
                        vetoException.printStackTrace();
                    } catch (FlowException flowException) {
                        flowException.printStackTrace();
                    }
                })
                .run();
    }

    @PreDestroy
    private void destroy() {
        sequentialTransition.stop();
    }


}


