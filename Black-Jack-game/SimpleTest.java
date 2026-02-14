public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("Testing basic functionality...");
        
        // Test creating a card
        com.KIRA_ZINA.app.Card card = new com.KIRA_ZINA.app.Card(com.KIRA_ZINA.app.Card.Suit.HEARTS, com.KIRA_ZINA.app.Card.Rank.ACE);
        System.out.println("Created card: " + card);
        System.out.println("Card value: " + card.getValue());
        
        // Test creating a deck
        com.KIRA_ZINA.app.Deck deck = new com.KIRA_ZINA.app.Deck();
        System.out.println("Created deck with " + deck.size() + " cards");
        
        // Test dealing a card
        com.KIRA_ZINA.app.Card dealtCard = deck.dealCard();
        System.out.println("Dealt card: " + dealtCard);
        
        System.out.println("Success! Basic functionality works.");
    }
}