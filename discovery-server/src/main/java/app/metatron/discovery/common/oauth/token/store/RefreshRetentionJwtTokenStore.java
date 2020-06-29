/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.metatron.discovery.common.oauth.token.store;

import app.metatron.discovery.common.oauth.token.cache.*;
import app.metatron.discovery.util.HttpUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 *
 */
public class RefreshRetentionJwtTokenStore extends JwtTokenStore {

  private static Logger LOGGER = LoggerFactory.getLogger(RefreshRetentionJwtTokenStore.class);

  @Autowired
  RefreshTokenCacheRepository refreshTokenCacheRepository;

  @Autowired
  AccessTokenCacheRepository accessTokenCacheRepository;

  @Autowired
  WhitelistTokenCacheRepository whitelistTokenCacheRepository;

  @Autowired
  HttpServletRequest httpServletRequest;

  /**
   * Create a JwtTokenStore with this token enhancer (should be shared with the DefaultTokenServices if used).
   */
  public RefreshRetentionJwtTokenStore(JwtAccessTokenConverter jwtTokenEnhancer) {
    super(jwtTokenEnhancer);
  }

  @Override
  public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
    super.storeAccessToken(token, authentication);

    String grantType = authentication.getOAuth2Request().getGrantType();
    if ("password".equals(grantType)
        || "authorization_code".equals(grantType)
        || authentication.getOAuth2Request().getRefreshTokenRequest() != null) {
      LOGGER.debug("Store Access Token with refresh token and white-list");

      OAuth2Request oAuth2Request = authentication.getOAuth2Request();

      // get user host IP address
      String userHost;
      try {
        userHost = HttpUtils.getClientIp(httpServletRequest);
      } catch (IllegalStateException ise) {
        userHost = getRemoteAddress();
      }

      //store access token
      accessTokenCacheRepository.putAccessToken(token.getValue(), token.getRefreshToken().getValue(),
                                                authentication.getName(), token.getExpiration(),
                                                oAuth2Request.getClientId(), userHost);

      accessTokenCacheRepository.putAccessTokenByRefreshToken(token.getValue(), token.getRefreshToken().getValue(),
                                                              authentication.getName(), token.getExpiration(),
                                                              oAuth2Request.getClientId(), userHost);

      //store access token to whitelist
      whitelistTokenCacheRepository.putWhitelistToken(token.getValue(), authentication.getName(),
                                                      oAuth2Request.getClientId(), userHost);

      //store refresh token
      OAuth2RefreshToken oAuth2RefreshToken = token.getRefreshToken();
      if (oAuth2RefreshToken != null && oAuth2RefreshToken instanceof ExpiringOAuth2RefreshToken) {
        ExpiringOAuth2RefreshToken expiringOAuth2RefreshToken = (ExpiringOAuth2RefreshToken) oAuth2RefreshToken;
        refreshTokenCacheRepository
            .putRefreshToken(expiringOAuth2RefreshToken.getValue(), expiringOAuth2RefreshToken.getExpiration());
      }
    } else {
      LOGGER.debug("GrantType({}) does not need to store cache", authentication.getOAuth2Request().getGrantType());
    }


  }

  /**
   * (for test code)
   * If the IllegalStateException error occurs, get the ip address as an alternative
   *
   * @return
   */
  private String getRemoteAddress() {

    RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
    if (attribs instanceof NativeWebRequest) {
      HttpServletRequest request = (HttpServletRequest) ((NativeWebRequest) attribs).getNativeRequest();
      return request.getRemoteAddr();
    }

    return "127.0.0.1";
  }

  @Override
  public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
    super.storeRefreshToken(refreshToken, authentication);

    String grantType = authentication.getOAuth2Request().getGrantType();
    LOGGER.debug("GrantType : {}", grantType);
    if ("password".equals(grantType)
        || "authorization_code".equals(grantType)
        || authentication.getOAuth2Request().getRefreshTokenRequest() != null) {
      LOGGER.debug("Store Refresh Token");
      if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
        refreshTokenCacheRepository
            .putRefreshToken(refreshToken.getValue(), ((ExpiringOAuth2RefreshToken) refreshToken).getExpiration());
      }
    } else {
      LOGGER.debug("GrantType({}) does not need to store cache", authentication.getOAuth2Request().getGrantType());
    }
  }

  @Override
  public OAuth2RefreshToken readRefreshToken(String tokenValue) {
    OAuth2RefreshToken refreshToken = super.readRefreshToken(tokenValue);
    if(refreshToken instanceof ExpiringOAuth2RefreshToken){
      CachedRefreshToken cachedRefreshToken
          = refreshTokenCacheRepository.getCachedRefreshToken(refreshToken.getValue());
      if(cachedRefreshToken != null){
        LOGGER.debug("refresh token expiration replaced by cache : {}", cachedRefreshToken.getExpiration());
        RefreshRetentionToken refreshRetentionToken = new RefreshRetentionToken(refreshToken);
        refreshRetentionToken.setExpiration(cachedRefreshToken.getExpiration());
        return refreshRetentionToken;
      }
    }
    return refreshToken;
  }

  @Override
  public void removeAccessToken(OAuth2AccessToken token) {
    super.removeAccessToken(token);
    LOGGER.debug("Remove Access Token");
    //remove access token
    accessTokenCacheRepository.removeAccessToken(token.getValue());
    //accessTokenCacheRepository.removeAccessTokenByRefreshToken(token.getRefreshToken().getValue());
  }

  @Override
  public void removeRefreshToken(OAuth2RefreshToken token) {
    super.removeRefreshToken(token);
    LOGGER.debug("Remove Refresh Token");
    //remove whitelist token
    String refreshTokenKey = token.getValue();
    CachedAccessToken cachedAccessToken = accessTokenCacheRepository.getCachedAccessTokenByRefreshToken(refreshTokenKey);
    if(cachedAccessToken != null){
      whitelistTokenCacheRepository.removeWhitelistTokenByCachedAccessToken(cachedAccessToken);
    }

    //remove access token by refresh token
    accessTokenCacheRepository.removeAccessTokenByRefreshToken(refreshTokenKey);

    refreshTokenCacheRepository.removeRefreshToken(token.getValue());
  }

  @Override
  public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
    super.removeAccessTokenUsingRefreshToken(refreshToken);
    LOGGER.debug("Remove Access Token using Refresh Token");
    String refreshTokenKey = refreshToken.getValue();
    CachedAccessToken cachedAccessToken = accessTokenCacheRepository.getCachedAccessTokenByRefreshToken(refreshTokenKey);
    if(cachedAccessToken != null){
      accessTokenCacheRepository.removeAccessToken(cachedAccessToken.getToken());
    }
  }

  @Override
  public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
    try {
      OAuth2AccessToken oAuth2AccessToken = readAccessToken(authentication.getPrincipal().toString());
      if (accessTokenCacheRepository.getCachedAccessToken(oAuth2AccessToken.getValue()) != null) {
        return readAccessToken(accessTokenCacheRepository.getCachedAccessToken(oAuth2AccessToken.getValue()).getToken());
      } else {
        accessTokenCacheRepository.removeAccessToken(authentication.getPrincipal().toString());
      }
    } catch (Exception e) {
      return null;
    }

    return null;
  }

  public class RefreshRetentionToken implements ExpiringOAuth2RefreshToken{
    OAuth2RefreshToken nativeToken;
    Date expiration;

    public RefreshRetentionToken(OAuth2RefreshToken oAuth2RefreshToken) {
      this.nativeToken = oAuth2RefreshToken;
    }

    public void setExpiration(Date expiration) {
      this.expiration = expiration;
    }

    @Override
    public String getValue() {
      if(nativeToken != null)
        return nativeToken.getValue();
      return null;
    }

    @Override
    public Date getExpiration() {
      if(expiration != null)
        return expiration;

      if(nativeToken != null && nativeToken instanceof ExpiringOAuth2RefreshToken)
        return ((ExpiringOAuth2RefreshToken) nativeToken).getExpiration();

      return null;
    }
  }
}