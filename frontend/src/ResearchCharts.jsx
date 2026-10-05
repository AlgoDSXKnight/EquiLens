import {
  Bar,
  BarChart,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

export default function ResearchCharts({ variant, data }) {
  if (variant === 'growth') {
    return (
      <ResponsiveContainer width="100%" height={260}>
        <BarChart data={data}>
          <XAxis dataKey="metric" />
          <YAxis />
          <Tooltip />
          <Bar dataKey="value" name="Value" />
        </BarChart>
      </ResponsiveContainer>
    );
  }

  const isRevenueProfit = variant === 'revenue-profit';
  const firstKey = isRevenueProfit ? 'revenue' : 'eps';
  const secondKey = isRevenueProfit ? 'netProfit' : 'freeCashFlow';
  const firstName = isRevenueProfit ? 'Revenue' : 'EPS';
  const secondName = isRevenueProfit ? 'Net Profit' : 'Free Cash Flow';

  return (
    <ResponsiveContainer width="100%" height={280}>
      <LineChart data={data}>
        <XAxis dataKey="year" />
        <YAxis />
        <Tooltip />
        <Line type="monotone" dataKey={firstKey} name={firstName} dot />
        <Line type="monotone" dataKey={secondKey} name={secondName} dot />
      </LineChart>
    </ResponsiveContainer>
  );
}
