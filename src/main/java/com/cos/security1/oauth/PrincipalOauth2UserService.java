package com.cos.security1.oauth;

import com.cos.security1.auth.PrincipalDetails;
import com.cos.security1.model.User;
import com.cos.security1.oauth.provider.FacebookUserInfo;
import com.cos.security1.oauth.provider.GoogleUserInfo;
import com.cos.security1.oauth.provider.NaverUserInfo;
import com.cos.security1.oauth.provider.OAuth2UserInfo;
import com.cos.security1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    //수정해야함
    //private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final UserRepository userRepository;

    //구글로 부터 받은 userRequest 데이터에 대한 후처리 되는 함수
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("userRequest = " + userRequest);
        System.out.println("userRequest.getClientRegistration() = " + userRequest.getClientRegistration()); //registarationId로 어떤 OAuth로 로그인 했는지 확인
        System.out.println("userRequest.getAccessToken() = " + userRequest.getAccessToken());
        System.out.println("super.loadUser(userRequest).getAttributes() = " + super.loadUser(userRequest).getAttributes());

        //구글 로그인 버튼 클릭 -> 구글 로그인창 -> 로그인 완료 -> code를 리턴(Oauth-Client 라이브러리) -> AccessToken 요청
        //userRequest 정보 -> loadUser 함수 호출-> 구글로부터 회원 프로필 받음
        OAuth2User oAuth2User = super.loadUser(userRequest);

        OAuth2UserInfo oAuth2UserInfo = null;

        if (userRequest.getClientRegistration().getRegistrationId().equals("google")) {
            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
            System.out.println("구글 로그인 요청");
        } else if (userRequest.getClientRegistration().getRegistrationId().equals("facebook")) {
            oAuth2UserInfo = new FacebookUserInfo(oAuth2User.getAttributes());
            System.out.println("페이스북 로그인 요청");
        } else if (userRequest.getClientRegistration().getRegistrationId().equals("naver")) {
            oAuth2UserInfo = new NaverUserInfo((Map)oAuth2User.getAttributes().get("response"));
            System.out.println("네이버 로그인 요청");
        } else {
            System.out.println("우리는 구글과 페이스북만 지원해요");
        }


        //회원가입을 강제로 진행 (기존 oAuth2User 활용)
        //String provider = userRequest.getClientRegistration().getRegistrationId();
        //String providerId = oAuth2User.getAttribute("sub");
        //String username = provider + "_" + providerId;
        //String password = "";
        //String email = oAuth2User.getAttribute("email");
        //String role = "ROLE_USER";

        //회원가입을 강제로 진행 (변경 oAuth2UserInfo 활용)
        String provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();
        String username = provider + "_" + providerId;
        String password = "";
        String email = oAuth2UserInfo.getEmail();
        String role = "ROLE_USER";

        User userEntity = userRepository.findByUsername(username);

        if (userEntity == null) {
            userEntity = User.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .role(role)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userRepository.save(userEntity);
        } else {
            System.out.println("로그인을 이미 한적이 있습니다. 당신은 자동회원가입이 되었습니다.");
        }

        //return super.loadUser(userRequest);
        return new PrincipalDetails(userEntity, oAuth2User.getAttributes());
    }
}
