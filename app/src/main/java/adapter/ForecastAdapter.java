package adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.climo.R;

import java.util.List;

import model.ForecastItem;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    // Lista de datos del clima
    private final List<ForecastItem> forecastList;

    // Constructor
    public ForecastAdapter(List<ForecastItem> forecastList) {
        this.forecastList = forecastList;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        ForecastItem item = forecastList.get(position);
        holder.tvTime.setText(item.getTime());
        holder.tvTemperature.setText(item.getTemperature());
        holder.ivIcon.setImageResource(item.getIcon());
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    // Clase interna para el ViewHolder
    public static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTemperature;
        ImageView ivIcon;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTemperature = itemView.findViewById(R.id.tvTemperature);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
