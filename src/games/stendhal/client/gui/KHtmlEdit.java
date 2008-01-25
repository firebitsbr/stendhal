/*
 * @(#) src/games/stendhal/client/gui/KHtmlEdit.java
 *
 * $Id$
 */

package games.stendhal.client.gui;

//
//

import games.stendhal.client.StendhalClient;
import games.stendhal.client.StendhalUI;
import games.stendhal.common.NotificationType;

import java.awt.Color;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import javax.swing.JTextPane;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import marauroa.common.game.RPAction;

/**
 * A HTML implementation of a KTextEdit component.
 * 
 * TODO: Many of the general HTML functions can be moved to a common utility
 * class.
 * 
 * TODO: Move the message formatting (and setup) code to a common class so that
 * the in-game text bubbles can use the same code for rendering.
 */
@SuppressWarnings("serial")
public class KHtmlEdit extends KTextEdit {
	//
	// KHtmlEdit
	//

	/**
	 * Handle hypertext link activation.
	 * 
	 * @param ev
	 *            The link event data.
	 */
	protected void activateLink(final HyperlinkEvent ev) {
		String text;
		URL url = ev.getURL();

		if (url != null) {
			if (url.getProtocol().equals("say")) {
				text = url.getPath();

				try {
					text = URLDecoder.decode(text, "UTF-8");
				} catch (UnsupportedEncodingException ex) {
					// Leave text as-is and hope for best
				}
			} else {
				// TODO: Activate browser (in a portable way)
				getToolkit().beep();
				return;
			}
		} else {
			text = ev.getDescription();

			if (text.startsWith("say:")) {
				text = text.substring(4);

				try {
					text = URLDecoder.decode(text, "UTF-8");
				} catch (UnsupportedEncodingException ex) {
					// Leave text as-is and hope for best
				}
			}
		}

		/*
		 * Chat link
		 */
		RPAction rpaction = new RPAction();

		rpaction.put("type", "chat");
		rpaction.put("text", text);

		StendhalClient.get().send(rpaction);
	}

	/**
	 * Append HTML text to the end of the content. Note: Currently elements must
	 * be complete to be added correctly.
	 * 
	 * @param text
	 *            The HTML text to add.
	 */
	protected void appendString(final String text) {
		HTMLDocument doc = (HTMLDocument) textPane.getDocument();

		try {
			Element root = doc.getParagraphElement(0);
			doc.insertBeforeEnd(root, text);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Append a character to a buffer, escaping HTML meta-characters when
	 * needed.
	 * @param sbuf 
	 * @param ch 
	 * 
	 */
	protected void appendHTML(final StringBuilder sbuf, final char ch) {
		switch (ch) {
		case '<':
			sbuf.append("&lt;");
			break;

		case '>':
			sbuf.append("&gt;");
			break;

		case '&':
			sbuf.append("&amp;");
			break;

		default:
			sbuf.append(ch);
			break;
		}
	}

	/**
	 * Escape text as HTML, escaping meta-characters.
	 * @param sbuf 
	 * 
	 * @param text
	 *            Raw text.
	 * 
	 */
	protected void appendHTML(final StringBuilder sbuf, final String text) {
		StringCharacterIterator ci = new StringCharacterIterator(text);
		char ch = ci.current();

		while (ch != CharacterIterator.DONE) {
			appendHTML(sbuf, ch);
			ch = ci.next();
		}
	}

	/**
	 * Translate a standard Stendhal encoded to HTML encoded.
	 * 
	 * @param text
	 *            The text to encode.
	 * 
	 * @return HTML encoded text.
	 */
	protected String translateToHTML(final String text) {
		StringBuilder sbuf = new StringBuilder();

		StringCharacterIterator ci = new StringCharacterIterator(text);
		char ch = ci.current();

		while (ch != CharacterIterator.DONE) {
			// display text after "#" as link
			if (ch == '#') {
				ch = ci.next();

				/*
				 * '##' means just a single '#'
				 */
				if (ch == '#') {
					appendHTML(sbuf, ch);
					ch = ci.next();
				} else {
					String link = extractLink(ci);

					/*
					 * Emit link (if any)
					 */
					if (link != null) {
						buildLink(sbuf, link);
					}

					ch = ci.current();
				}
			} else {
				appendHTML(sbuf, ch);
				ch = ci.next();
			}
		}

		return sbuf.toString();
	}

	/**
	 * Extract link content from a character iterator. It is assumed that the
	 * '#' has already been eaten. It leaves the character iterator at the first
	 * character after the link text.
	 * 
	 * @param ci
	 *            The character iterator.
	 * 
	 * @return Link text (or an empty string).
	 */
	protected String extractLink(final CharacterIterator ci) {
		StringBuilder sbuf = new StringBuilder();
		char ch = ci.current();
		char terminator = ' ';

		// color quoted compound words like "#'iron sword'"
		if (ch == '\'') {
			terminator = ch;
		}

		while (ch != CharacterIterator.DONE) {
			if (ch == terminator) {
				if (terminator == ' ') {
    				/*
    				 * Continued link (#abc #def)?
    				 */
    				ch = ci.next();

    				if (ch == '#') {
    					ch = ' ';
    				} else {
    					ci.previous();
    					break;
    				}
				} else {
					break;
				}
			}

			sbuf.append(ch);
			ch = ci.next();
		}

		/*
		 * Don't treat word delimiter(s) on the end as link text
		 */
		int len = sbuf.length();

		while (len != 0) {
			if (!isWordDelim(sbuf.charAt(--len))) {
				len++;
				break;
			}

			sbuf.setLength(len);
			ci.previous();
		}

		/*
		 * Nothing found?
		 */
		if (len == 0) {
			return null;
		}

		return sbuf.toString();
	}

	/**
	 * Determine is a character is a word delimiter when followed by a space or
	 * end-of-line. Care should be taken to avoid matching characters that are
	 * typically at the end of valid URL's.
	 * 
	 * @param ch
	 *            A character;
	 * 
	 * @return <code>true</code> if a word delimiter.
	 */
	protected boolean isWordDelim(char ch) {
		switch (ch) {
		case '.':
		case ',':
		case '!':
		case '?':
		case ';':
			return true;

		default:
			return false;
		}
	}

	/**
	 * Convert a text "link" to an HTML link. For well-known URL's, the link is
	 * taken literally, otherwise a <code>say:</code> URL will be generated.
	 * 
	 * @param sbuf
	 *            The string buffer to append to.
	 * @param text
	 *            The text to convert.
	 */
	protected void buildLink(StringBuilder sbuf, String text) {
		sbuf.append("<a href='");

		if (text.startsWith("http://") || text.startsWith("https://")
				|| text.startsWith("ftp://")) {
			sbuf.append(text);
		} else {
			sbuf.append("say:");

			try {
				sbuf.append(URLEncoder.encode(text, "UTF-8"));
			} catch (UnsupportedEncodingException ex) {
				// Nothing left to try
				sbuf.append(text);
			}
		}

		sbuf.append("'>");
		appendHTML(sbuf, text);
		sbuf.append("</a>");
	}

	/**
	 * Convert a color to a CSS color attribute value.
	 * 
	 * @param color
	 *            An AWT color.
	 * 
	 * @return A <code>color:</code> CSS attribute value.
	 */
	protected String colorToRGB(final Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(),
				color.getBlue());
	}

	//
	// KTextEdit
	//

	/**
	 * Build the GUI.
	 */
	@Override
	protected void buildGUI() {
		super.buildGUI();

		textPane.addHyperlinkListener(new ActivateLinkCB());
	}

	/**
	 * Initialize style information for a text pane.
	 * 
	 * @param textPane
	 *            The text pane.
	 */
	@Override
	protected void initStylesForTextPane(final JTextPane textPane) {
		textPane.setContentType("text/html");

		HTMLDocument doc = (HTMLDocument) textPane.getDocument();
		StyleSheet css = doc.getStyleSheet();

		/*
		 * Configure standard styles
		 */
		css.addRule("body { font-family: Dialog; font-size: " + (TEXT_SIZE + 1)
				+ "pt }");
		css.addRule("a { color: blue; font-style: italic }");

		css.addRule("._timestamp { color: " + colorToRGB(HEADER_COLOR)
				+ "; font-size: " + (TEXT_SIZE - 1)
				+ "pt; font-style: italic }");
		css.addRule("._header { color: " + colorToRGB(HEADER_COLOR) + " }");

		/*
		 * Configure notification types
		 */
		j2DClient ui = (j2DClient) StendhalUI.get();

		for (NotificationType type : NotificationType.values()) {
			Color color = ui.getNotificationColor(type);

			if (color != null) {
				css.addRule("." + type.getMnemonic() + " { color: "
						+ colorToRGB(color) + "; font-weight: bold; }");
			}
		}
	}

	@Override
	protected void insertHeader(final String text) {
		if ((text != null) && (text.length() != 0)) {
			StringBuilder sbuf = new StringBuilder();

			sbuf.append("<span class='_header'>");
			sbuf.append("&lt;");
			appendHTML(sbuf, text);
			sbuf.append("&gt;");
			sbuf.append("</span>");

			appendString(sbuf.toString());
		}
	}

	@Override
	protected void insertNewline() {
		appendString("<br>\n");
	}

	/**
	 * Insert the text portion of the line using a specified notification type
	 * for style.
	 * 
	 * @param text
	 *            The text to insert.
	 * @param type
	 *            The notification type.
	 */
	@Override
	protected void insertText(final String text, final NotificationType type) {
		StringBuilder sbuf = new StringBuilder();

		sbuf.append("<span class='");
		sbuf.append(type.getMnemonic());
		sbuf.append("'>");
		sbuf.append(translateToHTML(text));
		sbuf.append("</span>");

		appendString(sbuf.toString());
	}

	@Override
	protected void insertTimestamp(final String text) {
		StringBuilder sbuf = new StringBuilder();

		sbuf.append("<span class='_timestamp'>");
		appendHTML(sbuf, text);
		sbuf.append("</span>");

		appendString(sbuf.toString());
	}

	//
	//

	/**
	 * A hyperlink listener for link activation.
	 */
	protected class ActivateLinkCB implements HyperlinkListener {
		public void hyperlinkUpdate(HyperlinkEvent ev) {
			if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				activateLink(ev);
			}
		}
	}
}
