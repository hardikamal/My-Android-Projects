package crypsis.net.fourtytwotrips_driver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

public class CustomListAdapter extends ArrayAdapter<Map<String, Object>>{

    Context context;
    int layoutResourceId;
    ArrayList<Map<String,Object>> listItems;

    public CustomListAdapter(Context context, int layoutResourceId, ArrayList<Map<String, Object>> listItems) {
        super(context, layoutResourceId, listItems);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.listItems = listItems;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // 1. Create inflater
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 2. Get rowView from inflater
        View rowView = inflater.inflate(R.layout.custom_list_item, parent, false);

        // 3. Get the two text view from the rowView
        TextView title = (TextView) rowView.findViewById(R.id.title);
        TextView subtitle = (TextView) rowView.findViewById(R.id.subtitle);

        // 4. Set the text for textView

        String []splitText = listItems.get(position).get("placeName").toString().split(",", 2);

        if ( splitText.length > 0 ){  title.setText(splitText[0]);    }

        if ( splitText.length > 1 ){  subtitle.setText(splitText[1]);    }

        // 5. retrn rowView
        return rowView;
    }
}
