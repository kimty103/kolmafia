package net.sourceforge.kolmafia.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChatFormatterTest {
  @BeforeEach
  public void resetHighlights() {
    Preferences.setString("highlightList", "");
    StyledChatBuffer.initializeHighlights();
  }

  @Test
  void removeLineColorTest() {
    assertEquals("test1", ChatFormatter.removeLineColor("<font color=red>test1</font>"));
    assertEquals("test2", ChatFormatter.removeLineColor("<font color=green>test2</font>"));
  }

  @Test
  void getNormalizedMessageTest() {
    assertEquals("test1", ChatFormatter.formatInternalMessage("test<!--lastseen:test-->1"));
    assertEquals(
        "test2", ChatFormatter.formatInternalMessage("test<table><tr>tr</tr><td>td</td></table>2"));
  }

  @Test
  void removeMessageColorsTest() {
    assertEquals(
        "testRemoveColor",
        ChatFormatter.removeMessageColors("test<font color=red>Remove</font>Color"));
  }

  @Test
  void formatChatMessageTest() {
    String sender = "testSender";
    String content = "testContent";

    String channel = "testChannel";
    String messageType = "testMessageType";
    String userId = "testUserId";

    String hexColor =
        "\"#" + Integer.toHexString(ChatFormatter.getRandomColor().getRGB()).substring(2) + "\"";

    Pattern CHAT_MESSAGE_PATTERN =
        Pattern.compile("who=" + sender + ".+>" + sender + "<.+: " + content);
    Pattern SYSTEM_MESSAGE_PATTERN = Pattern.compile("who=-1.+>System Message<.+: " + content);
    Pattern MODERATOR_MESSAGE_PATTERN =
        Pattern.compile("who=" + userId + ".+>" + messageType + "<.+: " + content);
    Pattern EVENT_MESSAGE_PATTERN = Pattern.compile("color=" + hexColor + ">" + content + "<");

    ChatMessage chatMessage = new ChatMessage();
    SystemMessage systemMessage = new SystemMessage(content);
    ModeratorMessage moderatorMessage = new ModeratorMessage(channel, messageType, userId, content);
    EventMessage eventMessage = new EventMessage(content, hexColor);

    chatMessage.setSender(sender);
    chatMessage.setContent(content);

    assertTrue(CHAT_MESSAGE_PATTERN.matcher(ChatFormatter.formatChatMessage(chatMessage)).find());
    assertTrue(
        SYSTEM_MESSAGE_PATTERN.matcher(ChatFormatter.formatChatMessage(systemMessage)).find());
    assertTrue(
        MODERATOR_MESSAGE_PATTERN
            .matcher(ChatFormatter.formatChatMessage(moderatorMessage))
            .find());
    assertTrue(EVENT_MESSAGE_PATTERN.matcher(ChatFormatter.formatChatMessage(eventMessage)).find());
  }

  @Test
  public void addHightlightTest() {
    Color black = Color.BLACK;
    Color white = Color.WHITE;

    // Here we're testing if the messages are added correctly
    ChatFormatter.addHighlighting("test case", black);
    // The first highlight, this is intended to be overwritten
    ChatFormatter.addHighlighting("a test", black);
    // The second highlight, this is also intended to be overwritten
    ChatFormatter.addHighlighting("test", black);
    ChatFormatter.addHighlighting("a test with a long message", black);
    // This should remove the second highlight and replace it
    ChatFormatter.addHighlighting("a test", black);
    // This should remove the first highlight and replace it with white
    ChatFormatter.addHighlighting("test", white);
    ChatFormatter.addHighlighting("the test", black);

    String expected =
        "test case\n#000000\na test with a long message\n#000000\na test\n#000000\ntest\n#ffffff\nthe test\n#000000";

    assertEquals(expected, Preferences.getString("highlightList"));
  }

  @Test
  public void removeHighlightTest() {
    Color color = Color.BLACK;

    // Here we're checking if the highlight is removed from the preferences correctly
    // The first message starts with the name of the highlight
    ChatFormatter.addHighlighting("test case", color);
    // This ends with the name of the highlight
    ChatFormatter.addHighlighting("a test", color);
    // This simply contains the highlight message
    ChatFormatter.addHighlighting("a test with a long message", color);
    // This is the highlight we intend to remove
    ChatFormatter.addHighlighting("test", color);
    // A message added on, to prove that it doesn't wipe out following highlights
    ChatFormatter.addHighlighting("another test", color);

    ChatFormatter.removeHighlighting("test");
    String expected =
        "test case\n#000000\na test\n#000000\na test with a long message\n#000000\nanother test\n#000000";

    assertEquals(expected, Preferences.getString("highlightList"));
  }
}
