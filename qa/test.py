from mysetup import *

def test_missing_main_end_brace():
	code = '''exec {
		const avogadro := 6.022E+23.
		const pi := 3.14159.
		const twoPi := pi + pi.
		print twoPi; _n_.
		print avogadro; _n_.
		print pi; _n_.
	'''
	assert "expecting [CLOSE_BRACE]" in compile_error_string(code)

def test_short():
	code = '''exec {
		const avogadro := 6.022E+23.
		const pi := 3.14159.
		const twoPi := pi + pi.
		print twoPi; _n_.
		print avogadro; _n_.
		print pi; _n_.
	}'''

	expected = '''6.28318 
6.022e+023 
3.14159 
'''
	assert expected == compile_run_string(code)

def test_long():
	code = '''exec {
		const heh:=10.
		print heh*2, _n_.
		print heh*2 + 3 - 3 * 3, _n_.
		print heh*2+(3- 3)*3,_n_.
		print heh*2 + -3 - 3 * -3, _n_.

		var hi :=220.
		var charhi:=^\^.

		print hi,charhi, _n_.
		print _n_.

		var a:="hehe".
		var b:="hehe".
		var c:="hehe".
		print "all should be false:";a==b;b==c;a==c,_n_.
		c:=a.
		print "first true, second false:";c==a;c==b,_n_.
		b:=c.
		print "all should be true:";a==b;b==c;a==c,_n_.
		a:="hehe".
		print "only middle true:";a==b;b==c;a==c,_n_.
		b:=a.
		print "only first true:";a==b;b==c;a==c,_n_.

		print hi;charhi,_t_,[hi|bool]==[[hi|int]|bool],_t_,[hi|bool]==[[hi|char]|bool],_n_.

		#heh:=10.
}'''

	expected = '''20
14
20
26
220\\

all should be false: false false false
first true, second false: true false
all should be true: true true true
only middle true: false true false
only first true: true false false
220 \\\ttrue\ttrue
'''

	assert expected == compile_run_string(code)