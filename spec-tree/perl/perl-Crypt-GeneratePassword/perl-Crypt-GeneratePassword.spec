Name:           perl-Crypt-GeneratePassword
Version:        0.03
Release:        13%{?dist}
Summary:        Generate secure random pronounceable passwords
License:        GPL+ or Artistic
Group:          Development/Libraries
URL:            http://search.cpan.org/dist/Crypt-GeneratePassword/
Source0:        http://www.cpan.org/modules/by-module/Crypt/Crypt-GeneratePassword-%{version}.tar.gz
Patch0:         utf8.patch
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
BuildArch:      noarch
Requires:       perl(:MODULE_COMPAT_%(eval "`%{__perl} -V:version`"; echo $version))
BuildRequires:  perl(ExtUtils::MakeMaker)
%description
Crypt::GeneratePassword generates random passwords that are (more or less)
pronounceable. Unlike Crypt::RandPasswd, it doesn't use the FIPS-181 NIST
standard, which is proven to be insecure. It does use a similar interface,
so it should be a drop-in replacement in most cases.

%prep
%setup -q -n Crypt-GeneratePassword-%{version}
%patch0 -p1

%build
%{__perl} Makefile.PL INSTALLDIRS=vendor
make %{?_smp_mflags}

%install
rm -rf $RPM_BUILD_ROOT

make pure_install PERL_INSTALL_ROOT=$RPM_BUILD_ROOT

find $RPM_BUILD_ROOT -type f -name .packlist -exec rm -f {} \;
find $RPM_BUILD_ROOT -depth -type d -exec rmdir {} 2>/dev/null \;

%{_fixperms} $RPM_BUILD_ROOT/*

%check
make test

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%doc Changes README
%{perl_vendorlib}/*
%{_mandir}/man3/*

%changelog
* Thu Oct 16 2008 Milan Zazrivec 0.03-13
- Bumping release to be above what we have in Satellite 5.2.0

* Tue Oct 14 2008 Miroslav Suchý <msuchy@redhat.com> 0.03-3
- Specfile autogenerated by cpanspec 1.77.
