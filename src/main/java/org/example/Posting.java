    package org.example;

    import java.util.ArrayList;
    import java.util.List;

    public class Posting
    {
        int documentID;
        List<Integer> positions;

        public Posting(int ID)
        {
            this.documentID = ID;
            this.positions = new ArrayList<>();
        }

        public void AddPosition(int position)
        {
            this.positions.add(position);
        }

        public int GetTF()
        {
            return positions.size();
        }
    }
