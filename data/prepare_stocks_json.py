import json

path_to_stocks = "tesla_stocks.json"

with open(path_to_stocks, 'r') as f:
	data = json.load(f)
	
new_json = []

for el in data.items():
	el[1]["date"] = el[0]
	
	# Renaming all keys
	el[1]['open'] = float(el[1].pop('1. open'))
	el[1]['high'] = float(el[1].pop('2. high'))
	el[1]['low'] = float(el[1].pop('3. low'))
	el[1]['close'] = float(el[1].pop('4. close'))
	el[1]['adjusted_close'] = float(el[1].pop('5. adjusted close'))
	el[1]['volume'] = float(el[1].pop('6. volume'))
	el[1]['dividend_amount'] = float(el[1].pop('7. dividend amount'))
	el[1]['split_coefficient'] = float(el[1].pop('8. split coefficient'))
	
	transformed_el = el[1]
	new_json.append(transformed_el)


result_filename = 'tesla_stocks_final.json'

with open(result_filename, 'w') as f:
	json.dump(new_json, f)