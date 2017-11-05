package drivers;

import models.network.NetClient;
import models.packets.*;
import models.packets.base.AnswerStatus;
import models.packets.base.BaseRequest;
import models.resolvers.*;
import models.resolvers.base.BaseResolver;

import java.util.Hashtable;

public class PacketRouter {

    private Hashtable<Integer, BaseResolver> requestResolvers;

    public PacketRouter() {

        this.requestResolvers = new Hashtable<>();

        this.requestResolvers.put(new RequestRegister().getRequestCode(), new RegisterResolver());
        this.requestResolvers.put(new RequestLogin().getRequestCode(), new LoginResolver());
        this.requestResolvers.put(new RequestFollow().getRequestCode(), new FollowResolver());
        this.requestResolvers.put(new RequestUnFollow().getRequestCode(), new UnFollowResolver());
        this.requestResolvers.put(new RequestPostTweet().getRequestCode(), new PostTweetResolver());
        this.requestResolvers.put(new RequestGetTweets().getRequestCode(), new GetTweetsResolver());
        this.requestResolvers.put(new RequestSearchUserTitle().getRequestCode(), new SearchUserTitleResolver());
        this.requestResolvers.put(new RequestGetHumanById().getRequestCode(), new GetHumanByIdResolver());
        this.requestResolvers.put(new RequestGetFollowers().getRequestCode(), new GetFollowersResolver());
        this.requestResolvers.put(new RequestGetFollowings().getRequestCode(), new GetFollowingsResolver());
        this.requestResolvers.put(new RequestEditUserBio().getRequestCode(), new EditBioResolver());
        this.requestResolvers.put(new RequestLikeTweet().getRequestCode(), new LikeTweetResolver());
        this.requestResolvers.put(new RequestUnlikeTweet().getRequestCode(), new UnlikeTweetResolver());
        this.requestResolvers.put(new RequestGetTopTweets().getRequestCode(), new GetTopTweetsResolver());
        this.requestResolvers.put(new RequestGetFollowRequests().getRequestCode(), new GetFollowRequestsResolver());
        this.requestResolvers.put(new RequestAcceptFollowRequest().getRequestCode(), new AcceptFollowRequestResolver());
        this.requestResolvers.put(new RequestIgnoreFollowRequest().getRequestCode(), new IgnoreFollowRequestResolver());
        this.requestResolvers.put(new RequestGetFeed().getRequestCode(), new GetFeedResolver());
        this.requestResolvers.put(new RequestSwitchProfileMode().getRequestCode(), new SwitchProfileModeResolver());
        this.requestResolvers.put(new RequestDeleteTweet().getRequestCode(), new DeleteTweetResolver());
    }

    void directPacket(NetClient netClient, Object rawRequest) {

        if (rawRequest instanceof BaseRequest) {

            BaseRequest baseRequest = (BaseRequest) rawRequest;

            BaseResolver resolver = this.requestResolvers.get(baseRequest.getRequestCode());

            if (resolver != null) {
                resolver.resolvePacket(netClient, baseRequest);
            } else {
                AnswerRequest answerRequest = new AnswerRequest();
                answerRequest.packetCode = baseRequest.packetCode;
                answerRequest.answerStatus = AnswerStatus.ERROR_100;
                netClient.getConnection().sendTCP(answerRequest);
            }
        }
    }
}