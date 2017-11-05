package models.resolvers;

import drivers.MainDriver;
import models.network.NetClient;
import models.packets.AnswerEditUserBio;
import models.packets.RequestEditUserBio;
import models.packets.base.AnswerStatus;
import models.resolvers.base.BaseResolver;

import java.sql.PreparedStatement;

public class EditBioResolver extends BaseResolver {

    @Override
    public void resolvePacket(NetClient netClient, Object packet) {

        RequestEditUserBio requestEditUserBio = (RequestEditUserBio) packet;

        if (netClient.isAuthenticated()) {

            if (requestEditUserBio.newBio != null && requestEditUserBio.newBio.length() > 0) {

                try {
                    String query = "update UsersTitles set UserBio = ? where HumanId = ?";
                    PreparedStatement prpStmt = MainDriver.getInstance().getDatabaseDriver().getSqlDB().prepareStatement(query);
                    prpStmt.setString(1, requestEditUserBio.newBio);
                    prpStmt.setLong(2, netClient.getDbHumanNodeId());
                    prpStmt.executeUpdate();
                    prpStmt.close();
                    MainDriver.getInstance().getDatabaseDriver().getSqlDB().commit();

                    AnswerEditUserBio answerEditUserBio = new AnswerEditUserBio();
                    answerEditUserBio.answerStatus = AnswerStatus.OK;
                    answerEditUserBio.packetCode = requestEditUserBio.packetCode;
                    netClient.getConnection().sendTCP(answerEditUserBio);

                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
            else {

                AnswerEditUserBio answerEditUserBio = new AnswerEditUserBio();
                answerEditUserBio.answerStatus = AnswerStatus.ERROR_1000;
                answerEditUserBio.packetCode = requestEditUserBio.packetCode;
                netClient.getConnection().sendTCP(answerEditUserBio);
            }
        }
        else {

            AnswerEditUserBio answerEditUserBio = new AnswerEditUserBio();
            answerEditUserBio.answerStatus = AnswerStatus.ERROR_1001;
            answerEditUserBio.packetCode = requestEditUserBio.packetCode;
            netClient.getConnection().sendTCP(answerEditUserBio);
        }
    }
}